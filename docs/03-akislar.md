# Uçtan Uca Akışlar

Diyagramlarda `Sim` external-sim'deki ilgili sahte kurumu temsil ediyor.

## 1. Kayıt ve KYC

```mermaid
sequenceDiagram
    actor M as Müşteri
    participant App as iOS
    participant ID as identity
    participant C as customer
    participant K as kyc
    participant Sim as Sim (SMS, NVİ, MASAK)

    M->>App: Telefon numarası
    App->>ID: POST /v1/auth/otp
    ID->>Sim: SMS gönder
    M->>App: OTP kodu
    App->>App: Secure Enclave'de anahtar çifti üret
    App->>ID: POST /v1/auth/register (OTP, device public key)
    ID-->>App: access + refresh token. KYC seviye 0
    Note over App: Yemek / taksi kartla kullanılabilir

    M->>App: "Cüzdanı aç": TCKN, ad soyad, doğum tarihi
    App->>C: POST /v1/customers
    C->>K: CustomerRegistered
    K->>Sim: NVİ teyidi
    K->>Sim: Yaptırım taraması
    alt hepsi temiz
        K-->>App: Seviye 1 (push)
        Note over App: Cüzdan ve sanal IBAN açılır
    else isim benzerliği
        K->>K: Manuel inceleme kuyruğu
    else NVİ uyuşmazlığı
        K-->>App: Red + gerekçe
    end

    M->>App: "Finans'ı kullan"
    App->>App: Kimlik NFC okuma (simülatörde mock) + canlılık
    App->>K: POST /v1/kyc/remote-identification
    K-->>App: Seviye 2
```

## 2. Kartla para yükleme (3DS)

```mermaid
sequenceDiagram
    actor M as Müşteri
    participant App as iOS
    participant P as payment
    participant L as ledger
    participant Sim as Sim (PSP)

    App->>P: POST /v1/topups (tutar, kart, Idempotency-Key)
    P->>P: Limit + risk kontrolü
    P->>Sim: Ödeme başlat
    Sim-->>P: 3DS gerekli, redirect URL
    P-->>App: status=REQUIRES_3DS, url
    App->>Sim: WebView'de 3DS sayfası
    M->>Sim: SMS kodu (sim'de sabit: 123456)
    Sim->>P: Sonuç callback'i
    P->>L: Yevmiye: D PSP alacağı / C müşteri cüzdanı
    P-->>App: SUCCEEDED (push + ekran güncellemesi)
```

Kart sonucu belirsizse (timeout) ödeme `UNKNOWN` olur, bakiye **artmaz**; PSP'ye sorgu atılarak netleştirilir.

## 3. Sanal IBAN'a gelen FAST

```mermaid
sequenceDiagram
    participant Sim as Sim (FAST)
    participant T as transfer
    participant W as wallet
    participant L as ledger
    participant N as notification

    Sim->>T: Gelen transfer bildirimi (fastRef, alıcı IBAN, tutar, gönderen)
    T->>T: fastRef daha önce işlendi mi? (unique)
    T->>W: Sanal IBAN → cüzdan
    alt cüzdan aktif ve limit uygun
        T->>L: D FAST takas hesabı / C müşteri cüzdanı
        T->>N: IncomingTransferReceived
        N-->>N: Push: "Hesabına 500 TL geldi"
    else cüzdan kapalı / limit aşımı
        T->>Sim: İade (aynı IBAN'a geri)
    end
```

## 4. IBAN'a para gönderme (giden FAST)

```mermaid
sequenceDiagram
    actor M as Müşteri
    participant App as iOS
    participant T as transfer
    participant L as ledger
    participant Sim as Sim (FAST)

    App->>App: Face ID → isteği cihaz anahtarıyla imzala
    App->>T: POST /v1/transfers/iban (imzalı, Idempotency-Key)
    T->>T: İmza, limit, risk
    T->>L: Blokaj (kullanılabilir bakiyeden düş)
    T->>Sim: FAST gönder
    alt başarılı
        T->>L: Blokajı tahsil et: D müşteri cüzdanı / C FAST takas hesabı
    else alıcı IBAN kapalı
        T->>L: Blokajı serbest bırak
    else timeout
        T->>T: UNKNOWN, sorgu job'u netleştirir
    end
```

Para çekme IBAN'a gittiği için geri alınamaz. Bu yüzden önce blokaj, sonuç kesinleşince tahsil.

## 5. Yemek checkout'u ve BNPL

```mermaid
sequenceDiagram
    actor M as Müşteri
    participant Food as FoodMiniApp
    participant Co as commerce
    participant P as payment
    participant Cr as credit
    participant Ld as lending
    participant Sim as Sim (Banka A)

    Food->>Co: Sipariş oluştur (450 TL)
    Co->>P: PaymentIntent (450 TL, orderRef)
    P-->>Food: Ödeme yöntemleri: cüzdan, kart, sonra öde
    M->>Food: "Sonra öde – 3 taksit"
    Food->>P: Intent'i onayla (method=BNPL, term=3)
    P->>Cr: Drawdown iste
    Cr->>Cr: Limit yeterli mi, gecikme var mı
    Cr->>Ld: Kullandırım
    Ld->>Sim: Banka A API
    Sim-->>Ld: Onay + ödeme planı
    Cr->>Cr: Drawdown ACTIVE, plan kaydı, limit -450
    Cr->>P: DrawdownDisbursed
    P->>P: Intent SUCCEEDED, restoran alacağı
    P->>Co: PaymentIntentSucceeded
    Co-->>Food: Sipariş onaylandı
```

## 6. Taksi: ön provizyon ve tahsil

```mermaid
sequenceDiagram
    participant Taxi as TaxiMiniApp
    participant Co as commerce
    participant P as payment
    participant L as ledger

    Taxi->>Co: Yolculuk talebi (tahmini 150 TL)
    Co->>P: PaymentIntent (capture=MANUAL, 150 TL)
    P->>L: Blokaj 150 TL
    Note over Taxi: Yolculuk sürüyor
    Taxi->>Co: Yolculuk bitti (gerçek 132 TL)
    Co->>P: Capture 132 TL
    P->>L: 132 TL tahsil, 18 TL blokaj serbest
```

Gerçek tutar blokajı aşarsa (trafik) fark ayrıca tahsil edilir; bakiye yetmezse kayıtlı karta düşer.

## 7. Kredi değerlendirme ve limit açılışı

```mermaid
sequenceDiagram
    actor M as Müşteri
    participant App as iOS
    participant Cr as credit
    participant Cs as consent
    participant Sim as Sim (KKB, Banka)
    participant Ld as lending

    M->>App: Finans sekmesi
    App->>Cs: KKB sorgu izni + KVKK açık rıza
    App->>Cr: POST /v1/credit/assessments
    Cr->>Cr: Kesin kurallar (yaş, KYC seviye 2, yaptırım)
    Cr->>Sim: KKB skoru
    Cr->>Cr: İç veri skoru (sipariş düzenliliği, hesap yaşı, iade oranı)
    Cr->>Ld: Hangi banka? (lender_routing_policy)
    Cr->>Cr: Politika matrisi → teklif
    Cr-->>App: Teklif: 10.000 TL, aylık %3,49, max 12 ay, 7 gün geçerli
    M->>App: Kabul et
    App->>Cr: Başvuru
    Cr-->>App: Ön bilgilendirme formu
    M->>App: Sözleşme + OTP onayı
    Cr->>Ld: Limit açılışı
    Ld->>Sim: Banka API
    Sim-->>Ld: Onay (banka referansı)
    Cr-->>App: credit_line ACTIVE
```

## 8. Otomatik tahsilat

```mermaid
sequenceDiagram
    participant Job as Tahsilat job'u (vade günü)
    participant Col as collection
    participant W as wallet
    participant L as ledger
    participant Ld as lending
    participant Sim as Sim (Banka)

    Job->>Col: Bugün vadesi gelen taksitler
    loop her taksit
        Col->>W: Kullanılabilir bakiye?
        alt yeterli
            Col->>L: D müşteri cüzdanı / C bankaya aktarılacak tahsilat
            Col->>Ld: Ödeme bildirimi (anlık)
            Ld->>Sim: "X kullanımının 1. taksiti ödendi"
        else yetersiz
            Col->>Col: collection_attempt = INSUFFICIENT_FUNDS
            Col-->>Col: Push "Bakiye yükle". Gün içinde tekrar dene
        end
    end
    Note over Job: Gün sonu
    Job->>Col: Toplu aktarım
    Col->>Sim: Tek FAST + mutabakat dosyası
```

Para yüklendiği anda (`WalletCredited`) bekleyen tahsilat varsa hemen denenir; müşteri akşamı beklemez.

## 9. Banka değişimi

```mermaid
sequenceDiagram
    actor Ops as Operasyon
    participant Ld as lending
    participant Cr as credit

    Note over Ld: 01.11'e kadar yeni limitler Banka A
    Ops->>Ld: lender_routing_policy: 01.11'den itibaren Banka B
    Note over Cr: Mevcut credit_line'lar lender_id=A ile devam eder
    Cr->>Ld: (02.11) yeni değerlendirme: hangi banka?
    Ld-->>Cr: Banka B
    Note over Cr: Aynı müşterinin eski limiti A'da, yenisi B'de olabilir mi?<br/>Hayır: müşteri başına tek aktif limit (ADR-003)
```
