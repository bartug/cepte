# ADR-003: Banka kredi başına seçilir, bankalar anti-corruption layer arkasında

- **Durum:** Kabul edildi
- **Tarih:** 15.10.2026

## Bağlam

Krediyi biz vermiyoruz; anlaşmalı banka veriyor (lender of record). Bugün Banka A ile çalışıyoruz, yarın Banka B'ye
geçebiliriz ya da ikisiyle birlikte çalışabiliriz. Banka değişse bile eski bankanın verdiği krediler vade sonuna kadar
o bankada kalır. Bankaların API'leri birbirinden farklı.

## Karar

1. "Hangi banka" global bir ayar değil. Her `application`, `credit_line` ve `drawdown` kendi `lender_id`'sini taşır.
2. Yeni limitlerin hangi bankaya gideceğine `lender_routing_policy` karar verir (geçerlilik tarihleri ve öncelik ile).
3. Domain, bankaları `LenderGateway` port'u üzerinden görür. Her banka için ayrı adapter; bankanın alan adları, durum
   kodları ve hata formatı adapter'ın dışına sızmaz.
4. Müşteri başına aynı anda **tek aktif kredi limiti** olur. Banka değişiminde eski limit kapanmadan yenisi açılmaz.
5. Kredinin gerçek durumu (bakiye, faiz, gecikme) bankadadır. Bizdeki kayıt bir aynadır; webhook, gün sonu dosyası ve
   anlık sorgu ile güncellenir ve mutabakatla doğrulanır.

## Değerlendirilen alternatifler

| Alternatif | Neden seçilmedi |
|---|---|
| Aktif banka config'te | Banka değiştiğinde eski kredilerin tahsilatı yanlış bankaya bildirilir. |
| Müşteri başına birden fazla limit (her bankadan bir) | Toplam risk takibi, checkout'ta hangi limitin kullanılacağı ve tahsilat sırası karmaşıklaşır. Gerekirse ileride. |
| Bankanın modelini doğrudan domain'de kullanmak | İkinci banka geldiğinde domain her iki bankanın kavramlarını bilmek zorunda kalır. |

## Sonuçlar

- (+) Banka değişimi bir veri değişikliği; deploy gerektirmez.
- (+) Yeni banka eklemek yeni bir adapter yazmak demek.
- (−) Ayna veri ile bankadaki gerçek arasında fark olabilir; mutabakat zorunlu.
