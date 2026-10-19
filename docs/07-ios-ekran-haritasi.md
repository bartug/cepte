# iOS Ekran Haritası

## 1. Uygulama iskeleti

```
Açılış
 ├─ Oturum yok ──────────► Onboarding akışı (tam ekran)
 └─ Oturum var ──► Kilit ekranı (Face ID / PIN) ──► Ana tab bar

Ana tab bar
 ├─ Ana Sayfa    : bakiye kartı, hızlı işlemler, mini app ızgarası, kampanyalar
 ├─ Cüzdan       : bakiye, IBAN, işlem geçmişi, yükle / gönder / çek
 ├─ QR           : ortadaki büyük buton, TR Karekod okut
 ├─ Finans       : limit, kullanımlar, ödeme planı (seviye 2 değilse KYC'ye yönlendirir)
 └─ Profil       : kişisel bilgiler, cihazlar, güvenlik, izinler, bildirimler
```

Yemek ve taksi tab değil, **mini app**: Ana Sayfa'daki ızgaradan açılır, kendi navigasyon yığınıyla tam ekran
çalışır, sağ üstteki kapat butonuyla süper uygulamaya döner. Ödeme anında ortak **checkout sayfası** (sheet) açılır.

## 2. Akışlar ve ekranlar

### Onboarding ve KYC

| Ekran | Not |
|---|---|
| Karşılama | 3 sayfalık tanıtım, "Başla" |
| Telefon | Ülke kodu, numara maskesi |
| OTP | 6 hane, otomatik doldurma (`.oneTimeCode`), yeniden gönder sayacı |
| PIN oluştur | 6 hane, tekrar |
| Face ID izni | `NSFaceIDUsageDescription`; reddedilirse PIN ile devam |
| Kimlik bilgileri | TCKN (algoritma kontrolü istemcide de), ad, soyad, doğum tarihi |
| Kontrol ediliyor | Sonuç push ile gelir; ekran polling + push ile güncellenir |
| Kimlik kartı okut | NFC talimatları. Simülatörde **mock okuyucu**: test kişisini seç |
| Canlılık | Kamera yönlendirmesi. Simülatörde mock: "başarılı / başarısız" |
| İzinler | KVKK, KKB, ticari ileti; metin görüntüleme + onay |
| Sonuç | Seviye ve açılan özellikler |

### Cüzdan

| Ekran | Not |
|---|---|
| Cüzdan ana | Bakiye (gizle/göster), kullanılabilir / bloke ayrımı, IBAN kopyala/paylaş |
| İşlem geçmişi | Sonsuz kaydırma, tarih grupları, filtre. Offline önbellekten açılır, arkada yenilenir |
| İşlem detayı | Dekont, karşı taraf, durum, "itiraz et" |
| Para yükle | Tutar → kart seç / yeni kart → 3DS (WebView) → sonuç |
| Para gönder | Alıcı (rehber, kayıtlı, IBAN) → tutar → özet → Face ID → sonuç |
| QR ödeme | Kamera (`VisionKit DataScannerViewController`), simülatörde örnek QR seçimi |

### Finans

| Ekran | Not |
|---|---|
| Finans ana | Limit yoksa: "Limitini öğren". Varsa: kullanılabilir limit halkası, yaklaşan taksit |
| Değerlendirme | İzin kontrolü → yükleniyor → teklif ya da ret gerekçesi |
| Teklif | Limit, oran, YMO, örnek ödeme planı hesaplayıcı (vade kaydırıcı) |
| Ön bilgilendirme ve sözleşme | **Server-driven**: adımlar ve metinler backend'den gelir |
| Sözleşme onayı | OTP |
| Kullanımlar | Aktif kullanımlar, her birinin ödeme planı |
| Ödeme planı | Taksit listesi, durumları, "şimdi öde", "erken kapat" |
| Nakit kullanım | Tutar, vade → plan önizleme → onay |
| Otomatik ödeme | Talimat durumu, aç/kapat |

### Mini app'ler

| Mini app | Ekranlar |
|---|---|
| Yemek | Restoran listesi → menü → sepet → ortak checkout → sipariş takibi |
| Taksi | Harita yerine sahte konum seçimi → tahmini ücret → "çağır" (blokaj) → yolculuk → bitiş, tahsil |

### Ortak checkout (sheet)

```
┌──────────────────────────────┐
│  Burger House     450,00 TL  │
│  ○ Cüzdan (1.200,00 TL)       │
│  ○ •••• 0002                  │
│  ● Sonra öde                  │
│      ○ 30 gün sonra tek ödeme │
│      ● 3 taksit × 163,81 TL   │
│  [        Öde        ]        │
└──────────────────────────────┘
```

Ödeme yöntemleri backend'deki `payment_intent`'ten gelir; "sonra öde" sadece aktif ve yeterli limit varsa listelenir.

### Geliştirici menüsü (sadece Debug build)

Cihazı sallayınca ya da profilde 5 kez dokununca açılır.

| Bölüm | İşlem |
|---|---|
| Ortam | Backend URL, sertifika sabitleme aç/kapat |
| Zaman | +1 gün, +1 ay, belirli tarihe git (`/sim/clock`) |
| Para | Sanal IBAN'ıma FAST gönder, bankaya doğrudan ödeme yap |
| Kaos | Banka A / KKB / PSP kapat |
| Bildirimler | Gönderilen SMS ve push listesi; OTP'yi kopyala |
| Oturum | Token'ları sil, refresh token'ı boz (SEC-01'i dene) |
| Feature flag | Mini app'leri aç/kapat |

## 3. Navigasyon

- Her tab'ın kendi `NavigationStack`'i ve router'ı var. Router, tip güvenli `Route` enum'u tutar.
- Deeplink örnekleri:
  - `cepte://wallet/transactions/{id}`: push'tan işlem detayı
  - `cepte://finance/installments/{id}/pay`: "taksidin bugün" push'undan ödeme
  - `cepte://food/restaurants/{id}`: kampanyadan restorana
- Deeplink, KYC seviyesi yetmeyen bir yere giderse önce KYC akışı açılır, bitince hedefe devam edilir.
- Oturum süresi dolarsa (refresh başarısız) kullanıcı kilit ekranına döner; bulunduğu yer kaybolmaz.

## 4. Durumlar

Her ekranın ele alması gereken durumlar ortak bir `LoadState` ile modellenir:

| Durum | Görünüm |
|---|---|
| `idle` / `loading` | İskelet (skeleton) görünüm |
| `loaded` | İçerik |
| `empty` | Boş durum illüstrasyonu + aksiyon |
| `failed` | Hata mesajı (backend'in `ResponseMessage.message` alanı) + "tekrar dene" |
| `offline` | Önbellekteki veri + "çevrimdışı" bandı |
