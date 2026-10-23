# ADR-006: Cihaza bağlı oturum, tek aktif cihaz

- **Durum:** Kabul edildi
- **Tarih:** 23.10.2026

## Bağlam

Cüzdan uygulamasında giriş, e-ticaretten daha yüksek risk taşır: SMS OTP SIM kopyalamaya açık, çalınan bir token doğrudan
para çıkışı demek. Müşteri telefonla kayıt oluyor; şifre yok.

## Karar

1. **Kayıt:** telefon + OTP + 6 haneli PIN. PIN backend'de Argon2id ile hash'lenir, cihazda saklanmaz.
2. **Cihaz bağlama:** kayıtta cihazın Secure Enclave'inde P-256 anahtar üretilir, public key backend'e kaydedilir.
   Giriş, token yenileme ve para çıkışı istekleri bu anahtarla imzalanır.
3. **Tek aktif cihaz:** yeni cihaz bağlamak OTP + PIN ister; eski cihaz ve tüm oturumları kapanır. Veritabanında
   kullanıcı başına tek `ACTIVE` cihaz partial unique index ile korunur.
4. **Token'lar:** access token JWT (ES256, 15 dk); refresh token opak, tek kullanımlık, 30 gün, aile ile rotation.
   Kullanılmış token tekrar gelirse aile iptal edilir.
5. **Face ID:** ayrı bir biyometrik anahtar. Face ID sonucu backend'e gitmez; backend sadece imzayı doğrular.
6. **Uygulama kilidi:** 5 dakikadan uzun arka planda kalan uygulama kilitlenir; açmak için PIN ya da Face ID ile backend'e giriş yapılır.

## Değerlendirilen alternatifler

| Alternatif | Neden seçilmedi |
|---|---|
| Sadece SMS OTP ile giriş | SIM kopyalayan kişi hesabı tamamen ele geçirir. |
| Çoklu cihaz | Daha esnek ama "cihazlarım" yönetimi, cihaz başına bildirim ve risk kuralları gerekir. İleride eklenebilir; veri modeli (cihaz başına aile) buna hazır. |
| Uzun ömürlü access token, refresh yok | Çalınan token iptal edilemez. |
| PIN'i cihazda doğrulamak (Keychain) | Cihaz ele geçirilirse PIN deneme sınırı uygulanamaz; sınırın sunucuda olması gerekir. |
| HMAC ile istek imzası (payment-switch gibi) | Secret iki tarafta da durur; backend sızıntısında cihaz adına imza atılabilir. |
| OAuth2 / Keycloak gibi bir kimlik sunucusu | Kavramları (rotation, cihaz bağlama) kendi kodumuzda görmek projenin amacı. Gerçek bir üründe değerlendirilir. |

## Sonuçlar

- (+) Refresh token sızsa bile cihaz anahtarı olmadan kullanılamaz.
- (+) SIM kopyalama tek başına hesabı ele geçirmeye yetmez.
- (−) Her istekte cihazın aktif olup olmadığı veritabanından okunur; trafik arttığında kısa ömürlü cache gerekir.
- (−) Lokalde JWT anahtarı açılışta üretilir; backend yeniden başlayınca access token'lar geçersiz olur, uygulama yeniler.
- (−) Kullanıcı telefonunu kaybederse yeni cihazda OTP + PIN ile girer; PIN'i de unuttuysa müşteri hizmetleri akışı gerekir (kapsam dışı).
