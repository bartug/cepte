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
| PostgreSQL | `localhost:5433` (payment-switch ile çakışmasın diye 5433) |
| Redis | `localhost:6380` |

### Step 2: Backend ve simülatör

```bash
./mvnw install -DskipTests
./mvnw -pl backend spring-boot:run        # 8080
./mvnw -pl external-sim spring-boot:run   # 8091
```

Ya da container olarak:

```bash
docker compose --profile app up -d --build
```

Flyway migration'ları backend açılırken çalışır.

| | Adres |
|---|---|
| Backend Swagger | http://localhost:8080/swagger-ui.html |
| Simülatör Swagger | http://localhost:8091/swagger-ui.html |
| Modül yapısı | http://localhost:8080/actuator/modulith |

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
| `BACKEND_URL` | `http://localhost:8080` | Simülatörün saati bildirdiği adres |
| `INTERNAL_API_TOKEN` | `local-dev-only-internal-token` | Simülatör → backend `/internal/**` çağrıları. **Production'da zorunlu** |
| `MINIMUM_APP_VERSION` | `1.0.0` | Bu sürümün altındaki iOS uygulamaları güncellemeye zorlanır |
| `LOG_LEVEL` | `INFO` | |
| `SPRING_PROFILES_ACTIVE` | - | `production`: Swagger kapanır, loglar ECS formatına geçer, saat sistem saati olur |

## 📊 İzleme

- Health: `GET /actuator/health/liveness`, `GET /actuator/health/readiness`
- Metrikler: `GET /actuator/prometheus`
- Modüller ve bağımlılıkları: `GET /actuator/modulith`
- Modül diyagramları: `./mvnw -pl backend test -Dtest=ModularityTest` → `backend/target/spring-modulith-docs/*.puml`

```bash
# Mini uygulama ve feature flag'ler
docker exec cepte-postgres psql -U cepte -d cepte -c "select code, enabled, sort_order from appconfig.mini_app order by sort_order"

# Mini app'i kapat (uygulama bir sonraki konfigürasyon okumasında ana sayfadan kaldırır)
curl -s -X PUT localhost:8080/v1/admin/app/mini-apps/TAXI -H 'Content-Type: application/json' -d '{"enabled": false}'
```

## 🚀 Deploy

Proje eğitim amaçlı, canlıya çıkmıyor. İmajlar yine production'a uygun hazırlanır: multi-stage build, layer'lara ayrılmış
jar, non-root kullanıcı.

```bash
docker build -f backend/Dockerfile -t cepte/backend:1.0.0 .
docker build -f external-sim/Dockerfile -t cepte/external-sim:1.0.0 .
```
