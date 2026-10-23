# Cepte

Süper uygulama mantığında bir cüzdan: bakiye, para yükleme ve gönderme, QR ile ödeme, uygulama içindeki yemek ve
taksi dikeylerinde ortak checkout, anlaşmalı banka üzerinden kredi limiti ve "sonra öde", otomatik taksit tahsilatı.

payment-switch'te paranın POS'tan bankaya nasıl gittiğini yazmıştım. Burada paranın müşterinin cebinde nasıl
durduğunu, nasıl hareket ettiğini ve kredinin nasıl verilip geri toplandığını yazıyorum. Proje eğitim amaçlı;
canlıya çıkmıyor ve hiçbir gerçek sisteme bağlanmıyor. Bankalar, NVİ, KKB, kart ödemesi ve FAST senaryolu bir
simülatörle taklit ediliyor.

```
┌─────────── iOS (SwiftUI) ───────────┐
│ Cüzdan • Finans • Yemek • Taksi      │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐        ┌──────────────── external-sim ────────────────┐
│ backend (Spring Modulith)            │ ─────► │ NVİ • MASAK • KKB • PSP • FAST               │
│ identity • kyc • wallet • ledger     │        │ Banka A • Banka B • SMS • Push               │
│ payment • transfer • credit • ...    │ ◄───── │ senaryo API'si • zaman makinesi              │
└──────────────────────────────────────┘        └──────────────────────────────────────────────┘
```

Domain sözlüğü: [docs/01-domain-sozlugu.md](docs/01-domain-sozlugu.md) ·
Mimari: [docs/02-mimari.md](docs/02-mimari.md) ·
Akışlar: [docs/03-akislar.md](docs/03-akislar.md) ·
Durum makineleri: [docs/04-durum-makineleri.md](docs/04-durum-makineleri.md) ·
Veri modeli: [docs/05-veri-modeli.md](docs/05-veri-modeli.md) ·
Test senaryoları: [docs/06-test-senaryolari.md](docs/06-test-senaryolari.md) ·
iOS ekranları: [docs/07-ios-ekran-haritasi.md](docs/07-ios-ekran-haritasi.md) ·
Java'dan Swift'e: [docs/08-javadan-swifte.md](docs/08-javadan-swifte.md) ·
Kimlik ve oturum: [docs/09-kimlik-ve-oturum.md](docs/09-kimlik-ve-oturum.md) ·
Kararlar: [docs/adr](docs/adr) · Ortam kurulumu: [DEVOPS.md](DEVOPS.md)

## 1. Geliştirme Ortamı

- Java 21, Maven 3.9 (wrapper projede mevcut), Docker
- Xcode 26 (iOS 18 SDK, iPhone 17 Pro simülatörü)
- IntelliJ IDEA + EnvFile eklentisi. `.env-template` dosyasını `.env` olarak kopyalayıp doldurun.

```bash
docker compose up -d
./mvnw clean verify
./mvnw -pl backend spring-boot:run
./mvnw -pl external-sim spring-boot:run
open ios/Cepte.xcodeproj
ios/scripts/test.sh
```

## 2. Yapı

| Klasör | İçerik |
|---|---|
| `common/` | Backend ve simülatörün ortak sınıfları: `ResponseMessage`, exception'lar, `Money`, `ControllableClock` |
| `backend/` | Java 21, Spring Boot 3, Spring Modulith, Spring Security, PostgreSQL, Redis. Modüller `com.bartugsevindik.cepte.backend.*` altında |
| `external-sim/` | Dış dünya simülatörü ve zaman makinesi |
| `ios/Cepte/` | Uygulama shell'i: açılış, tab bar, mini app konteyneri, bağımlılık kökü |
| `ios/Packages/` | `Core` (Networking, AppConfiguration, Navigation, DeviceSecurity, SessionKit), `DesignSystem`, `MiniAppKit`, `Features` (AuthFlow, Home, DevMenu, FoodMiniApp, TaxiMiniApp) |
| `docs/` | Analiz, akışlar, kararlar |

## 3. Kod Kuralları

- Backend'de zaman sadece enjekte edilen `Clock`'tan okunur; `Instant.now()` ArchUnit testiyle yasak (ADR-004).
- Modüller birbirinin sadece kök paketindeki tiplerini kullanır; `ModularityTest` ihlalde build'i kırar (ADR-001).
- iOS'ta feature paketleri birbirine bağımlı olamaz; aralarındaki geçişi shell yapar (ADR-005).
- Para her iki tarafta da en küçük birimde (`long` / `Int64` kuruş) taşınır.

## 4. Yol Haritası

| Faz | Kapsam | Durum |
|---|---|---|
| CP-0 | Analiz: domain, mimari, akışlar, durum makineleri, veri modeli, test senaryoları | ✅ |
| CP-1 | İskelet: backend + external-sim + compose, `Clock`; iOS paket yapısı, DesignSystem, App Shell, DevMenu | ✅ |
| CP-2 | Kimlik: OTP, oturum, refresh token rotation, cihaz bağlama; iOS kayıt, PIN, Face ID | ✅ |
| CP-3 | Müşteri, KYC seviyeleri, izinler; iOS KYC akışı | |
| CP-4 | Cüzdan ve ledger, limitler, sanal IBAN; iOS ana ekran ve işlem geçmişi | |
| CP-5 | Kartla yükleme (3DS), gelen ve giden FAST, P2P, QR; push bildirimleri | |
| CP-6 | Mini app'ler: yemek, taksi, ödeme niyeti, ön provizyon; ortak checkout | |
| CP-7 | Kredi: değerlendirme, teklif, limit, BNPL, nakit kullanım, Banka A / B, banka değişimi | |
| CP-8 | Geri ödeme: ödeme planı, otomatik tahsilat, gecikme, erken kapama | |
| CP-9 | Mutabakat ve backoffice | |
| CP-10 | Sağlamlaştırma: güvenlik, uçtan uca senaryolar, UI testleri | |
| CP-11 | Mülakat paketi | |
