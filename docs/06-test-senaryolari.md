# Test Senaryoları ve Simülatör

Hiçbir gerçek sisteme bağlanmıyoruz. `external-sim` dış dünyayı taklit ediyor; hangi senaryonun çalışacağını
**sihirli değerler** (Stripe'ın test kartları gibi) ya da senaryo kontrol API'si belirliyor.

Aynı senaryolar üç yerde kullanılır:
1. Backend entegrasyon testleri (WireMock ile aynı cevaplar),
2. Uçtan uca senaryo testleri (backend + external-sim),
3. iOS uygulamasındaki geliştirici menüsü (elle oynamak için).

## 1. Sihirli değerler

Tüm değerler geçerli formatta (TCKN algoritması, Luhn, IBAN mod-97); validasyondan geçer, sadece simülatör onları tanır.

### NVİ (kimlik teyidi)

| TCKN | Ad Soyad | Doğum tarihi | Sonuç |
|---|---|---|---|
| `10000000146` | Ayşe Yılmaz | 12.03.1992 | Eşleşir, yaptırım taraması temiz |
| `10000000214` | Mehmet Demir | 01.07.1988 | Eşleşir, **yaptırım listesinde isim benzerliği** → manuel inceleme |
| `10000000382` | Ali Kaya | 15.11.1975 | Eşleşir, **yaptırım listesinde kesin eşleşme** → red |
| `10000000450` | Zeynep Çelik | 23.05.2009 | Eşleşir, **18 yaş altı** → seviye 1 olur, kredi değerlendirmesi kesin kuralda reddeder |
| `10000000528` | Can Arslan | 30.09.1995 | **Doğum tarihi tutmaz** → red |
| `10000000696` | Elif Şahin | 04.02.1990 | Kişi **vefat etmiş** görünür → red |
| `10000000764` | Burak Öztürk | 19.08.1993 | NVİ **timeout** → tekrar denenir, sonra manuel inceleme |
| Diğer geçerli TCKN | herhangi | herhangi | Girilen bilgilerle eşleşir, temiz |

### KKB (kredi skoru)

KKB senaryosu TCKN'ye bağlı. Tabloda olmayan TCKN'ler orta skor alır.

| TCKN | KKB sonucu | Beklenen karar |
|---|---|---|
| `10000000146` | Skor 1650, aktif kredi yok | A bandı, yüksek limit |
| `10000000832` | **Kayıt yok (thin file)** | İç veri varsa C bandı küçük limit, yoksa red |
| `10000000214` | Skor 980 | Red: skor eşiğin altında |
| Diğer | Skor 1300 | B bandı |

Ek KKB senaryoları senaryo API'si ile: takipte kredi var, servis timeout.

### Kartlar (PSP)

| Kart numarası | Sonuç |
|---|---|
| `4000 0000 0000 0002` | Başarılı (3DS ile) |
| `4000 0000 0000 0010` | 3DS doğrulaması başarısız |
| `4000 0000 0000 0028` | Yetersiz bakiye |
| `4000 0000 0000 0036` | PSP timeout → `UNKNOWN`, sorguda **başarılı** çıkar |
| `4000 0000 0000 0044` | PSP timeout → `UNKNOWN`, sorguda **gerçekleşmemiş** çıkar |
| `5400 0000 0000 0005` | Başarılı, 3DS'siz (kayıtlı kartla tekrar yükleme) |
| `5400 0000 0000 0013` | Kart çalıntı bildirilmiş → red, risk kaydı |

3DS sayfasında SMS kodu her zaman `123456`; başka kod girilirse 3DS başarısız.

### IBAN (giden FAST)

Simülatördeki hayali "Dış Banka" kodu `00998`. Cepte'nin sanal IBAN'ları `00099` kodlu.

| Alıcı IBAN | Sonuç |
|---|---|
| `TR04 0099 8000 0000 0000 0000 01` | Başarılı, alıcı adı "AYŞE YILMAZ" |
| `TR74 0099 8000 0000 0000 0000 02` | Alıcı hesap kapalı → başarısız, blokaj serbest |
| `TR47 0099 8000 0000 0000 0000 03` | Timeout → `UNKNOWN`, sorguda başarılı |
| `TR84 0099 8000 0000 0000 0004 04` | Başarılı, sonra alıcı banka **iade** eder (1 dk sonra) |

### Bankalar (kredi)

Banka davranışı kullanım tutarının kuruş kısmıyla seçilir; böylece aynı müşteriyle farklı senaryolar denenebilir.

| Tutarın kuruşu | Banka A | Banka B |
|---|---|---|
| `,00` | Onay | Onay |
| `,13` | Red (banka iç kuralı) | Red |
| `,31` | Timeout → `UNKNOWN`, sorguda onaylı | Timeout → sorguda onaylı |
| `,37` | Timeout → `UNKNOWN`, sorguda kayıt yok | Timeout → sorguda kayıt yok |
| `,77` | Onay, ama ödeme planı bizim hesabımızdan 1 kuruş farklı | Aynı |

## 2. Senaryo kontrol API'si

Sihirli değerle ifade edilemeyen, olay tetikleyen senaryolar:

| Endpoint | Ne yapar |
|---|---|
| `POST /sim/fast/incoming` | Bir sanal IBAN'a gelen FAST tetikler (tutar, gönderen, `duplicate=true` ile iki kez) |
| `POST /sim/lenders/{bank}/direct-repayment` | Müşterinin bankaya doğrudan ödediğini bildirir (webhook) |
| `POST /sim/lenders/{bank}/chaos` | Bankayı kapat, yavaşlat, hata oranı ver |
| `POST /sim/lenders/{bank}/daily-file?mismatch=1` | Gün sonu bakiye dosyası üret (bilerek farklı kayıtla) |
| `POST /sim/kkb/chaos`, `/sim/nvi/chaos` | Servisi kapat / yavaşlat |
| `POST /sim/screening/list-update` | Yaptırım listesine kişi ekle (mevcut müşteri askıya düşmeli) |
| `POST /sim/clock/advance` | Zamanı ileri sar (`{"days": 31}` ya da `{"until": "2026-12-15T09:00"}`) |
| `GET /sim/sms`, `GET /sim/push` | Gönderilen SMS ve push'lar (OTP buradan okunur) |
| `POST /sim/reset` | Simülatör durumunu sıfırla |

## 3. Senaryo kataloğu

Her senaryo bir uçtan uca testtir. ID'ler test metot adlarında ve commit mesajlarında kullanılır.

### Onboarding ve KYC

| ID | Senaryo | Beklenen |
|---|---|---|
| KYC-01 | Ayşe kayıt olur, kimlik girer | Seviye 1, cüzdan + sanal IBAN açılır, push gelir |
| KYC-02 | Mehmet kimlik girer | Manuel inceleme; operatör onaylayınca seviye 1 |
| KYC-03 | Ali kimlik girer | Red, cüzdan açılmaz |
| KYC-04 | Can yanlış doğum tarihiyle | Red + gerekçe; 3 denemeden sonra 24 saat bekleme |
| KYC-05 | Burak, NVİ timeout | Otomatik tekrar; olmazsa manuel inceleme |
| KYC-06 | Ayşe uzaktan kimlik tespiti | Seviye 2, limitler yükselir |
| KYC-07 | Seviye 1 Ayşe sonradan yaptırım listesine eklenir | KYC `SUSPENDED`, cüzdan `FROZEN`, para çıkışı reddedilir |
| KYC-08 | Aynı TCKN ile ikinci kullanıcı | Red: TCKN başka hesaba bağlı |

### Kimlik ve güvenlik

CP-2'de yazıldı. Testler: `backend/.../identity/OtpIntegrationTest`, `SessionIntegrationTest`; iOS'ta `SessionManagerTests`,
`SessionStoreTests`, `OnboardingViewModelTests`, `RequestSignerTests`.

| ID | Senaryo | Beklenen | Test |
|---|---|---|---|
| AUTH-01 | Kayıtlı ve kayıtsız numaraya kod istenir | Cevaplar aynı | `kayitliVeKayitsizNumarayaAyniCevapDoner` |
| AUTH-02 | 5 hatalı kod | Doğru kod da reddedilir, yeni kod gerekir | `besHataliDenemedenSonraDogruKodDaReddedilir` |
| AUTH-03 | Kod 4 dk sonra girilir | Süresi dolmuş | `suresiDolanKodReddedilir` |
| AUTH-04 | 60 sn dolmadan yeni kod | 429 + Retry-After | `altmisSaniyeDolmadanYeniKodGonderilmez` |
| AUTH-05 | 10 dk'da 4. kod | 429 | `numaraBasinaPencereLimitiUygulanir` |
| AUTH-06 | Aynı biletle iki kayıt | İkincisi reddedilir | `biletTekKullanimlik` |
| AUTH-07 | Zayıf PIN (111111, 123456, 121212...) | Reddedilir, bilet yanmaz | `zayifPinReddedilirBiletYanmaz` |
| AUTH-08 | 5 yanlış PIN | 30 dk kilit; kilitliyken doğru PIN de 423 | `besYanlisPinSonrasiKilitlenir` |
| AUTH-09 | Yeni cihaz bağlanır | Eski cihazın access, refresh ve imzası geçersiz | `yeniCihazBaglanincaEskiCihazKapanir` |
| AUTH-10 | OTP ele geçirildi, PIN bilinmiyor (SIM kopyalama) | Cihaz bağlanamaz, gerçek kullanıcı etkilenmez | `simKopyalayanPiniBilmedenCihazBaglayamaz` |
| AUTH-11 | Face ID açılır, biyometrik imzayla giriş | Başarılı; biyometrik imza olmadan reddedilir | `faceIdAcilincaBiyometrikImzaIleGirilir` |
| AUTH-12 | Access token 16 dk sonra | 401 | `accessTokenOnBesDakikaSonraGecersiz` |
| SEC-01 | Kullanılmış refresh token tekrar gönderilir | Tüm aile iptal, `REFRESH_REUSE_DETECTED` | `kullanilmisRefreshTokenGelinceAileKapanir` |
| SEC-02 | Aynı nonce ile ikinci istek | "Bu istek daha önce kullanılmış." | `ayniImzaIkinciKezKullanilamaz` |
| SEC-03 | Zaman damgası 6 dk eski | 401 | `zamanDamgasiPencereDisindaysaReddedilir` |
| SEC-04 | Refresh başka cihazın anahtarıyla imzalı | 401 | `refreshTokenBaskaCihazinAnahtariylaKullanilamaz` |
| SEC-05 | İmzadan sonra gövde değiştirilir | 401 | `imzalandiktanSonraDegistirilenGovdeReddedilir` |
| SEC-06 | Denetim kaydı silinmeye çalışılır | Veritabanı trigger'ı reddeder | `denetimIziDegistirilemez` |
| SEC-07 | Aynı anda 5 istek 401 alır (iOS) | Tek yenileme | `eszamanli401lerTekYenilemeYapar` |

### Cüzdan ve para hareketleri

| ID | Senaryo | Beklenen |
|---|---|---|
| WAL-01 | `...0002` kartla 1.000 TL yükleme + 3DS | Bakiye 1.000 TL, ledger dengede |
| WAL-02 | `...0010` kartla yükleme | 3DS başarısız, bakiye değişmez |
| WAL-03 | `...0036` kartla yükleme | `UNKNOWN`; sorgu job'u sonrası bakiye artar |
| WAL-04 | `...0044` kartla yükleme | `UNKNOWN`; sorgu sonrası başarısız, bakiye değişmez |
| WAL-05 | Aynı Idempotency-Key ile iki yükleme isteği | Tek yükleme |
| WAL-06 | Seviye 1 günlük yükleme limitinin üstü | 422 limit aşımı |
| WAL-07 | Sanal IBAN'a 500 TL FAST | Bakiye artar, push gelir |
| WAL-08 | Aynı FAST bildirimi iki kez | Tek giriş |
| WAL-09 | Kapalı cüzdanın IBAN'ına FAST | Gönderene iade |
| WAL-10 | Ayşe → Mehmet 200 TL P2P | İki cüzdan anında güncellenir |
| WAL-11 | Aynı anda iki P2P, toplamı bakiyeyi aşıyor | Biri başarılı, biri yetersiz bakiye; bakiye negatife düşmez |
| WAL-12 | IBAN'a 300 TL, `...01` | Tamamlandı |
| WAL-13 | IBAN'a, `...02` | Başarısız, blokaj serbest, bakiye aynı |
| WAL-14 | IBAN'a, `...03` | `UNKNOWN` → sorgu → tamamlandı |
| WAL-15 | IBAN'a, `...0404` | Tamamlandı, 1 dk sonra iade, bakiye geri gelir |
| WAL-16 | Dondurulmuş cüzdandan transfer | Red; para girişi kabul edilir |

### Dikeyler

| ID | Senaryo | Beklenen |
|---|---|---|
| COM-01 | Yemek siparişi cüzdanla | Sipariş onaylı, restoran alacağı oluşur |
| COM-02 | Yemek, bakiye yetersiz → kartla tamamla | İlk deneme başarısız, intent kartla `SUCCEEDED` |
| COM-03 | Taksi 150 TL blokaj, 132 TL tahsil | Kullanılabilir bakiye yolculuk boyunca −150, sonunda −132 |
| COM-04 | Taksi gerçek tutar blokajı aşar (180 TL) | 30 TL fark tahsil edilir |
| COM-05 | Yolculuk iptal | Blokaj serbest |
| COM-06 | Yemek siparişi iadesi (kısmi) | Cüzdana kısmi iade |

### Kredi

| ID | Senaryo | Beklenen |
|---|---|---|
| CRD-01 | Ayşe (seviye 2, skor 1650) değerlendirme | A bandı teklif, Banka A |
| CRD-02 | Seviye 1 müşteri değerlendirme ister | Red: önce uzaktan kimlik tespiti |
| CRD-03 | KKB izni olmadan değerlendirme | Red: izin gerekli |
| CRD-04 | Thin file + 6 aylık düzenli sipariş geçmişi | C bandı küçük limit |
| CRD-05 | Thin file + sipariş geçmişi yok | Red |
| CRD-06 | Teklif 7 gün içinde kabul edilmez | `EXPIRED`; yeni değerlendirme gerekir |
| CRD-07 | Kabul + sözleşme OTP + banka onayı | `credit_line` ACTIVE, otomatik ödeme talimatı oluşur |
| CRD-08 | Banka limit açılışında timeout, sorguda onaylı | Önce `UNKNOWN`, sonra ACTIVE |
| CRD-09 | 450,00 TL BNPL, 3 taksit | Sipariş onaylı, limit −450, 3 taksitlik plan |
| CRD-10 | 450,13 TL BNPL | Banka reddi; intent başka yönteme döner |
| CRD-11 | 450,31 TL BNPL | `UNKNOWN`, limit düşük kalır; sorgu sonrası ACTIVE |
| CRD-12 | 450,37 TL BNPL | `UNKNOWN`; sorguda kayıt yok → REJECTED, limit geri açılır |
| CRD-13 | 450,77 TL BNPL | Plan bankadan alınır, bizim hesapla 1 kuruş fark → alarm |
| CRD-14 | Limit üstü kullanım | Checkout'ta "sonra öde" seçeneği görünmez |
| CRD-15 | 3.000 TL nakit kullanım, 6 ay | Cüzdana 3.000 TL, 6 taksitlik plan |
| CRD-16 | Cayma: 10. günde, kullanım yok | Limit `WITHDRAWN` |
| CRD-17 | Banka değişimi: politika 01.11'den Banka B | Eski müşterinin limiti A'da kalır; yeni müşteri B'ye gider |
| CRD-18 | Banka A kapalı (chaos) | Yeni değerlendirmeler B'ye failover (politika izin veriyorsa); A'daki limitlerin kullanımı reddedilir |

### Geri ödeme ve gecikme

| ID | Senaryo | Beklenen |
|---|---|---|
| COL-01 | Vadeden 3 gün önce | Hatırlatma push'u |
| COL-02 | Vade günü, bakiye yeterli | Tahsil, bankaya bildirim, limit anapara kadar açılır |
| COL-03 | Vade günü bakiye yetersiz, öğlen 500 TL FAST gelir | Sabah `INSUFFICIENT_FUNDS`, FAST sonrası anında tahsil |
| COL-04 | Vade günü hiç para gelmez | Vade +1 gecikme, limit `SUSPENDED`, push |
| COL-05 | Gecikmedeki taksit ödenir | Limit tekrar ACTIVE |
| COL-06 | Tahsilat job'u aynı gün iki kez çalışır | Tek tahsilat |
| COL-07 | İlk taksitten sonra ikinci taksit de ödenmez | Banka muacceliyet ihtarı webhook'u, limit `ACCELERATED` |
| COL-08 | +90 gün | `DEFAULTED`, takibe devir |
| COL-09 | Müşteri bankaya doğrudan öder | Webhook → `repayment` (DIRECT_TO_LENDER), aynı gün otomatik tahsilat yapılmaz |
| COL-10 | Hem cüzdandan tahsil edildi hem bankaya doğrudan ödendi | Fazla ödeme tespiti, cüzdana iade |
| COL-11 | Erken kapama | Tutar bankadan sorgulanır, tek seferde tahsil, drawdown `PAID_OFF` |
| COL-12 | Kısmi ödeme | Waterfall: önce faiz ve vergi, sonra anapara |

### Mutabakat

| ID | Senaryo | Beklenen |
|---|---|---|
| REC-01 | Gün sonu toplu aktarım | Tek FAST, dosyadaki toplam = tahsil edilenler |
| REC-02 | Üç taraflı mutabakat temiz | Tüm kayıtlar eşleşir |
| REC-03 | Banka gün sonu dosyasında bir kredi farklı bakiye | `recon_item` MISMATCH, alarm |
| REC-04 | Ledger projeksiyonu postings ile karşılaştırma | Fark yok |

## 4. Zaman makinesi ile örnek koşu

```
t0      Ayşe: KYC-01, KYC-06, CRD-07 (limit 10.000 TL)
t0      CRD-09: 450 TL BNPL, vade günleri t0+1ay, +2ay, +3ay
t0+27g  clock/advance → COL-01 hatırlatma push'u
t0+30g  clock/advance → COL-04 (bakiye yok) → gecikme, limit askıda
t0+32g  fast/incoming 500 TL → gecikmedeki taksit anında tahsil (COL-05)
t0+90g  kalan taksitler otomatik tahsil, drawdown PAID_OFF
```
