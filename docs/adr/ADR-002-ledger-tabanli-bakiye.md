# ADR-002: Bakiye ledger'dan türetilir

- **Durum:** Kabul edildi
- **Tarih:** 15.10.2026

## Bağlam

Cüzdanda para; yükleme, P2P, FAST, BNPL, taksi blokajı, taksit tahsilatı ve iadelerle hareket ediyor. "Bakiye neden
bu?" sorusu her an cevaplanabilmeli. Mutabakat, ledger'ın doğruluğuna dayanıyor.

## Karar

1. Her para hareketi çift taraflı bir yevmiye kaydıdır: en az iki posting, borç toplamı = alacak toplamı.
   Veritabanında deferred constraint trigger ile kontrol edilir.
2. `journal_entry` ve `posting` değiştirilemez. Düzeltme ters kayıtla yapılır.
3. Bakiye için `account_balance` projeksiyonu tutulur ve postings ile **aynı transaction'da** güncellenir. Okuma
   hızlı, yazma tutarlı. Gece job'u projeksiyonu postings toplamıyla karşılaştırır.
4. Blokaj ayrı bir `hold` kaydıdır; kullanılabilir bakiye = bakiye − aktif blokajlar.
5. Müşteri cüzdanı hesapları negatife düşemez (`allow_negative = false`, satır kilidi altında kontrol).
6. Her kaydın `idempotency_key`'i unique: aynı iş iki kez muhasebeleşmez.

## Değerlendirilen alternatifler

| Alternatif | Neden seçilmedi |
|---|---|
| `wallet.balance` kolonu, `balance = balance + x` | Bakiyenin geçmişi yok, hatayı bulmak imkânsız, mutabakat yapılamaz. |
| Bakiyeyi her okumada postings toplamından hesaplamak | Doğru ama işlem sayısı arttıkça yavaşlar. Snapshot + delta ile çözülebilir; projeksiyon daha basit. |
| Event sourcing | Ledger zaten bir event log. Tüm domain'i event sourced yapmak öğrenme hedefinin dışında. |

## Sonuçlar

- (+) Her kuruşun nereden gelip nereye gittiği izlenebilir.
- (+) Mutabakat ledger üzerinden yapılır.
- (−) Her para hareketi birden fazla satır yazar; aynı hesaba yoğun yazmada satır kilidi darboğaz olabilir
  (şirket hesapları için ileride alt hesaplara bölme).
