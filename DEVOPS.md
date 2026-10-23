# Cepte - DevOps

## 📋 Ön Gereksinimler

| Araç | Sürüm | Not |
|---|---|---|
| Java | 21+ | Maven wrapper projede (`./mvnw`) |
| Docker | 24+ | Postgres, Redis ve Testcontainers için |
| Xcode | 26+ | iOS 18 SDK ve iPhone simülatörü |

## 🐳 Docker ile Ayağa Kaldırma

### Step 1: Altyapı

```bash
docker compose up -d
```

| Servis | Adres |
|---|---|
| PostgreSQL | `localhost:5433` (payment-switch ile çakışmasın diye 5433; backend de aynı sebeple 8100) |
| Redis | `localhost:6380` (OTP sayaçları, imza nonce'ları) |

### Step 2: Backend ve simülatör

```bash
./mvnw install -DskipTests
./mvnw -pl backend spring-boot:run        # 8100
./mvnw -pl external-sim spring-boot:run   # 8091
```

Ya da container olarak:

```bash
docker compose --profile app up -d --build
```

Flyway migration'ları backend açılırken çalışır.

| | Adres |
|---|---|
| Backend Swagger | http://localhost:8100/swagger-ui.html |
| Simülatör Swagger | http://localhost:8091/swagger-ui.html |
| Modül yapısı | http://localhost:8100/actuator/modulith |

### Step 3: iOS

```bash
open ios/Cepte.xcodeproj      # iPhone 17 Pro seç, ⌘R
ios/scripts/test.sh           # tüm iOS testleri + uygulama derlemesi
```

### Step 4: Zaman makinesi

Saatin sahibi simülatör. İleri sarıldığında backend'e bildirir; backend kapalıysa açılışta saati simülatörden çeker.

```bash
curl -s localhost:8091/sim/clock
curl -s -X POST localhost:8091/sim/clock/advance -H 'Content-Type: application/json' -d '{"days": 31}'
curl -s -X POST localhost:8091/sim/clock/advance -H 'Content-Type: application/json' -d '{"until": "2026-12-15T09:00:00"}'
```

### Step 5: Giriş

Uygulamada telefon numarası gir; kod simülatörün SMS kutusuna düşer. Kod ekranındaki "Simülatörden kodu al" butonu ya da:

```bash
curl -s "localhost:8091/sim/sms?limit=1"
```

Face ID için simülatörde Features > Face ID > Enrolled ve Matching Face. Ayrıntılar: [docs/09-kimlik-ve-oturum.md](docs/09-kimlik-ve-oturum.md)

> Simülatörün saati bellekte. **Simülatör yeniden başlarsa saati sıfırlanır ama backend ileride kalır** (backend'in saati
> geri gitmez). Temiz bir başlangıç için ikisini birlikte yeniden başlat.

## ⚙️ Ortam Değişkenleri

`.env-template` dosyasındaki değişkenler kullanılır. Hepsinin lokal için varsayılan değeri var.

| Değişken | Varsayılan | Açıklama |
|---|---|---|
| `PG_HOST` / `PG_PORT` / `PG_DB` | `localhost` / `5433` / `cepte` | |
| `PG_USERNAME` / `PG_PASSWORD` | `cepte` / `cepte` | Production'da secret store'dan gelmeli |
| `PG_POOL_SIZE` | `20` | Hikari havuz boyutu |
| `CLOCK_MODE` | `SIMULATED` | `SIMULATED`: saat simülatöre bağlı. `production` profilinde her zaman `SYSTEM` |
| `SIMULATOR_URL` | `http://localhost:8091` | Backend'in saati çektiği adres |
| `BACKEND_URL` | `http://localhost:8100` | Simülatörün saati bildirdiği adres |
| `INTERNAL_API_TOKEN` | `local-dev-only-internal-token` | Simülatör → backend `/internal/**` çağrıları. **Production'da zorunlu** |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6380` | Redis kapalıysa OTP limitleri veritabanından sayılır |
| `OTP_HASH_SECRET` | `local-dev-only-otp-hash-secret` | OTP kodlarının HMAC secret'ı. **Production'da zorunlu** |
| `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` | boş | Base64 PKCS#8 / X.509, EC P-256. Boşsa açılışta geçici anahtar üretilir (backend yeniden başlayınca access token'lar geçersiz). **Production'da zorunlu**. Üretmek için aşağıya bak |
| `MINIMUM_APP_VERSION` | `1.0.0` | Bu sürümün altındaki iOS uygulamaları güncellemeye zorlanır |
| `LOG_LEVEL` | `INFO` | |
| `SPRING_PROFILES_ACTIVE` | - | `production`: Swagger kapanır, loglar ECS formatına geçer, saat sistem saati olur |

JWT anahtarı üretmek:

```bash
openssl ecparam -name prime256v1 -genkey -noout -out jwt.pem
openssl pkcs8 -topk8 -nocrypt -in jwt.pem -outform DER | base64     # JWT_PRIVATE_KEY
openssl ec -in jwt.pem -pubout -outform DER | base64                # JWT_PUBLIC_KEY
```

## 📊 İzleme

- Health: `GET /actuator/health/liveness`, `GET /actuator/health/readiness`
- Metrikler: `GET /actuator/prometheus`
- Modüller ve bağımlılıkları: `GET /actuator/modulith`
- Modül diyagramları: `./mvnw -pl backend test -Dtest=ModularityTest` → `backend/target/spring-modulith-docs/*.puml`

```bash
# Giriş denetim izi
docker exec cepte-postgres psql -U cepte -d cepte -c "select type, user_id, device_id, detail, occurred_at from identity.auth_event order by id desc limit 20"

# Mini uygulama ve feature flag'ler
docker exec cepte-postgres psql -U cepte -d cepte -c "select code, enabled, sort_order from appconfig.mini_app order by sort_order"

# Mini app'i kapat (uygulama bir sonraki konfigürasyon okumasında ana sayfadan kaldırır)
curl -s -X PUT localhost:8100/v1/admin/app/mini-apps/TAXI -H 'Content-Type: application/json' -d '{"enabled": false}'
```

## 🚀 Deploy

Proje eğitim amaçlı, canlıya çıkmıyor. İmajlar yine production'a uygun hazırlanır: multi-stage build, layer'lara ayrılmış
jar, non-root kullanıcı.

```bash
docker build -f backend/Dockerfile -t cepte/backend:1.0.0 .
docker build -f external-sim/Dockerfile -t cepte/external-sim:1.0.0 .
```
