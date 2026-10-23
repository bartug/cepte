# Mimari

## 1. Genel görünüm

```
┌──────────────────────────── iOS uygulaması (SwiftUI) ─────────────────────────────┐
│  App Shell: oturum, tab bar, router, deeplink, mini app kaydı, feature flag'ler    │
│  Onboarding+KYC • Cüzdan • Finans • Yemek (mini app) • Taksi (mini app) • Profil   │
│  Core: Networking • Security (Keychain, Secure Enclave, Face ID) • DesignSystem    │
└───────────────────────────────────┬───────────────────────────────────────────────┘
                                    │ HTTPS / JSON, push (APNs simülasyonu)
┌───────────────────────────────────▼───────────────────────────────────────────────┐
│                 backend: Spring Boot modüler monolit (Spring Modulith)            │
│  identity • customer • kyc • consent • wallet • ledger • payment • transfer       │
│  credit • lending • collection • risk • notification • reconciliation • commerce  │
│  PostgreSQL (modül başına şema) • Redis • kalıcı modül event'leri                 │
└───────────────────────────────────┬───────────────────────────────────────────────┘
                                    │ HTTP, sadece adapter'lar üzerinden
┌───────────────────────────────────▼───────────────────────────────────────────────┐
│               external-sim: dış dünya simülatörü (ayrı uygulama)                  │
│  NVİ • MASAK • KKB • PSP (kart + 3DS) • FAST ağı • Banka A • Banka B • SMS • Push  │
│  senaryo kontrol API'si • zaman makinesi                                          │
└───────────────────────────────────────────────────────────────────────────────────┘
```

| Uygulama | Port | Açıklama |
|---|---|---|
| `backend` | 8100 | Tüm iş mantığı. iOS'un konuştuğu tek API. |
| `external-sim` | 8091 | Gerçek dünyadaki kurumların sahte hali. Backend onu gerçek bir dış sistem gibi çağırır. |
| `ios` | - | `Cepte` uygulaması, simülatörde çalışır. |

## 2. Backend modülleri

Kararın gerekçesi: [ADR-001](adr/ADR-001-moduler-monolit.md)

| Modül | Sorumluluk | Kime bağımlı |
|---|---|---|
| `identity` | Kayıt, OTP, oturum, refresh token, cihaz bağlama, PIN | - |
| `customer` | Hukuki kişi bilgileri, PII şifreleme | identity |
| `kyc` | Seviyeler, NVİ, yaptırım taraması, NFC, canlılık, manuel inceleme | customer |
| `consent` | İzin metinleri ve onay kayıtları | customer |
| `ledger` | Hesaplar, yevmiye kayıtları, bakiye, blokaj | - |
| `wallet` | Cüzdan hesabı, sanal IBAN, limitler, dondurma | customer, kyc, ledger |
| `risk` | İşlem kuralları, velocity kontrolleri | - |
| `payment` | Kartla yükleme, ödeme niyeti, ön provizyon / tahsil, ödeme yöntemleri | wallet, ledger, risk |
| `transfer` | P2P, giden ve gelen FAST, QR ödeme | wallet, ledger, risk |
| `lending` | Anlaşmalı bankalar, ürünler, banka seçimi, banka adapter'ları | - |
| `credit` | Değerlendirme, teklif, başvuru, limit, kullanım, ödeme planı | customer, kyc, consent, lending, ledger |
| `collection` | Otomatik ödeme talimatı, tahsilat, gecikme, erken kapama | credit, wallet, lending |
| `reconciliation` | Toplu aktarım, mutabakat | ledger, lending, collection |
| `notification` | Push, SMS, uygulama içi bildirimler | - (sadece event dinler) |
| `commerce` | Sahte yemek ve taksi dikeyleri | payment |
| `backoffice` | Operasyon API'leri | hepsi (sadece public API'leri) |

### Bağımlılık kuralları

- Bir modül diğer modülün sadece **kök paketindeki** public tiplerini kullanabilir. `internal` alt paketleri, entity'leri
  ve repository'leri dışarıya kapalıdır. Spring Modulith bunu `ApplicationModules.verify()` testi ile zorlar.
- Döngüsel bağımlılık yasak. `credit` → `collection` yönüne ihtiyaç doğarsa bu bir event'tir, çağrı değil.
- Her modülün kendi Postgres şeması vardır. Bir modül başka bir şemaya SQL atmaz.
- "Sonuçtan haberdar olmak" ihtiyacı event ile çözülür: `wallet` bir ödeme yapıldığını `notification`'a çağrı ile
  söylemez, `notification` ilgili event'i dinler.

### Modüller arası event'ler

Modulith event'leri publish eden transaction ile birlikte kalıcı olarak yazar (event publication registry). Uygulama
listener çalışmadan çökerse event açılışta tekrar işlenir. payment-switch'teki outbox'ın framework tarafından
sağlanan hali.

| Event | Yayınlayan | Dinleyen |
|---|---|---|
| `CustomerRegistered` | customer | kyc, notification |
| `KycLevelChanged` | kyc | wallet (limitleri günceller), credit (değerlendirmeye uygunluk), notification |
| `WalletCredited` / `WalletDebited` | wallet | notification, risk |
| `IncomingTransferReceived` | transfer | notification |
| `PaymentIntentSucceeded` | payment | commerce (siparişi onaylar) |
| `CreditLineOpened` | credit | collection (talimatı başlatır), notification |
| `DrawdownDisbursed` | credit | collection (planı takibe alır), payment (BNPL ödemesini tamamlar) |
| `InstallmentDue` / `InstallmentOverdue` | collection | notification, credit (limiti dondurur) |
| `RepaymentReceived` | collection | credit (limiti açar), reconciliation |

## 3. Dış sistemlere erişim

Her dış sistemin backend'de bir **port**'u (interface) ve bir **adapter**'ı vardır. Domain kodu adapter'ı bilmez.

```
credit ──► LenderGateway (port) ──► BankAAdapter ──HTTP──► external-sim /bank-a/...
                                └─► BankBAdapter ──HTTP──► external-sim /bank-b/...
kyc    ──► IdentityRegistry (port) ──► NviAdapter ──HTTP──► external-sim /nvi/...
```

- İki bankanın API'si bilerek **farklı** tasarlanacak (alan adları, durum kodları, hata formatı). Anti-corruption
  layer'ın ne işe yaradığı ancak böyle görünür. Gerekçe: [ADR-003](adr/ADR-003-kredi-basina-banka.md)
- Dış çağrıların hepsinde timeout, circuit breaker ve gerektiğinde inquiry var (payment-switch'teki desenler).

## 4. Zaman

Kredi ve tahsilatın neredeyse tamamı tarihe bağlı. Bunu test edebilmek için:

- Backend'de **hiçbir kod** `Instant.now()` / `LocalDate.now()` çağırmaz. Her şey enjekte edilen `Clock` üzerinden.
- `local` ve `test` profillerinde `Clock`, external-sim'deki zaman makinesine bağlıdır. `POST /sim/clock/advance`
  zamanı ileri sarar ve zamanlanmış job'ları tetikler.
- `production` profilinde sistem saati kullanılır, zaman makinesi endpoint'leri yoktur.

Gerekçe: [ADR-004](adr/ADR-004-simulator-ve-zaman.md)

## 5. Güvenlik modeli

| Katman | Yöntem |
|---|---|
| Kimlik doğrulama | Telefon + OTP ile kayıt. Giriş: PIN ya da Face ID. |
| Oturum | Kısa ömürlü access token (JWT, 15 dk) + refresh token (30 gün, tek kullanımlık, rotation). Kullanılmış bir refresh token tekrar gelirse aynı ailedeki tüm token'lar iptal edilir. |
| Cihaz bağlama | Kayıtta cihazın Secure Enclave'inde P-256 anahtar çifti üretilir, public key backend'e kaydedilir. Para çıkışı yapan istekler bu anahtarla imzalanır. |
| Step-up | Yeni alıcıya ya da eşik üstü transferde Face ID / PIN tekrar istenir; imza buna bağlıdır. |
| PII | TCKN, doğum tarihi, kimlik görüntüleri AES-GCM ile şifreli. TCKN araması için HMAC değeri tutulur. Loglarda maskeli. |
| Yetki | Müşteri sadece kendi kaynaklarına erişir. Backoffice ayrı rollerle (OPERATOR, COMPLIANCE, ADMIN). |

## 6. iOS mimarisi

Gerekçe: [ADR-005](adr/ADR-005-ios-mimarisi.md). Ekranlar: [07-ios-ekran-haritasi.md](07-ios-ekran-haritasi.md)

```
ios/
├── Cepte/                       ← uygulama hedefi: @main, DI kurulumu, mini app kaydı
└── Packages/
    ├── Core/                    ← Networking, Security, Persistence, APIClient, FeatureFlags, Logging
    ├── DesignSystem/            ← renk/tipografi token'ları, ortak bileşenler
    ├── MiniAppKit/              ← MiniApp protokolü, router, deeplink sözleşmesi
    └── Features/
        ├── Onboarding, KYC, Home, Wallet, Transfer, TopUp, QRPay
        ├── Credit
        ├── FoodMiniApp, TaxiMiniApp
        └── Profile, DevMenu
```

Kurallar:
- Feature paketleri birbirine bağımlı olamaz. Sadece `Core`, `DesignSystem` ve `MiniAppKit`'e bağlanır.
- Feature'lar arası geçiş router üzerinden, tip güvenli `Route` değerleriyle yapılır.
- Mini uygulamalar cüzdana doğrudan erişemez. `MiniAppContext` üzerinden sadece izin verilen yetenekleri kullanır
  (ödeme başlat, kullanıcı bilgisi, bildirim).
- API istemcisi backend'in OpenAPI tanımından üretilir.
