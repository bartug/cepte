# ADR-001: Modüler monolit (Spring Modulith)

- **Durum:** Kabul edildi
- **Tarih:** 15.10.2026

## Bağlam

Projede 15 civarında iş alanı var: kimlik, KYC, cüzdan, ledger, ödeme, transfer, kredi, tahsilat, mutabakat...
Bunların çoğu aynı işlemde birlikte çalışıyor: BNPL bir ödemede `payment`, `credit`, `lending` ve `ledger` aynı
akışın parçası. Ekip tek kişi, trafik öğrenme amaçlı. Asıl zorluk domain modellemesi.

## Karar

1. Backend tek bir Spring Boot uygulaması, içinde net sınırlı modüller.
2. Sınırları **Spring Modulith** zorlar: modüller birbirinin sadece kök paketindeki public API'sini kullanır,
   `ApplicationModules.verify()` testi ihlalde build'i kırar.
3. Her modülün kendi Postgres şeması var, şemalar arası foreign key yok.
4. Modüller arası "haber verme" Modulith event'leriyle; event'ler publish eden transaction'la birlikte kalıcı yazılır.
5. Dış sistemler (bankalar, NVİ, KKB, PSP, FAST) ayrı bir `external-sim` uygulamasında.

## Değerlendirilen alternatifler

| Alternatif | Neden seçilmedi |
|---|---|
| Mikroservisler (payment-switch gibi) | BNPL gibi akışlarda 4 servis arası saga gerekirdi. Dağıtık sistemin maliyeti (deploy, tracing, eventual consistency) bu projenin öğretmek istediği şeyin önüne geçer. payment-switch bunu zaten gösterdi. |
| Klasik katmanlı monolit (controller/service/repository) | Modül sınırı yok; bir yıl sonra her şey her şeyi çağırır. Ayrı servise çıkarmak imkânsızlaşır. |
| Modül başına ayrı Maven modülü | Sınırı derleyici zorlar, ama Modulith'in event registry'si ve modül testleri yine gerekir. İlk aşamada paket bazlı sınır yeterli. |

## Sonuçlar

- (+) BNPL gibi çok modüllü işlemler tek DB transaction'ında tutarlı. Saga yok.
- (+) Modül sınırı test ile korunuyor; `credit` ileride ayrı servise çıkarılabilir.
- (+) Tek deploy, tek log akışı, kolay lokal geliştirme.
- (−) Tek bir modüldeki bellek sızıntısı ya da yavaş sorgu bütün uygulamayı etkiler.
- (−) Modüller bağımsız ölçeklenemez.
- (−) Şemalar arası FK olmadığı için referans bütünlüğü uygulama katmanında.
