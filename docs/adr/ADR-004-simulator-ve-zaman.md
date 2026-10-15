# ADR-004: Dış sistemler simülatörde, zaman soyutlanmış

- **Durum:** Kabul edildi
- **Tarih:** 15.10.2026

## Bağlam

Proje canlıya çıkmayacak; NVİ, KKB, PSP, FAST ve bankalara gerçek erişim yok. Yine de bu sistemlerin davranışları
(timeout, belirsiz sonuç, mükerrer bildirim, iade) projenin öğretmek istediği şeyin merkezinde. Kredi ve tahsilat
senaryoları aylara yayılıyor.

## Karar

1. Dış sistemler `external-sim` adlı ayrı bir Spring Boot uygulamasında. Backend onu gerçek bir dış sistem gibi
   HTTP ile çağırır; testlerde aynı davranışlar WireMock ile de verilir.
2. Senaryolar sihirli değerlerle (TCKN, kart numarası, IBAN, tutarın kuruş kısmı) ve senaryo kontrol API'siyle seçilir.
   Katalog: [06-test-senaryolari.md](../06-test-senaryolari.md)
3. Backend'de zamanı soran tek yer enjekte edilen `Clock`. `local` ve `test` profillerinde bu saat simülatördeki zaman
   makinesine bağlı; zamanlanmış job'lar ileri sarılan zamana göre tetiklenir.
4. Zaman makinesi ve senaryo API'leri `production` profilinde yüklenmez.

## Sonuçlar

- (+) "Vade +31 gün" gibi senaryolar saniyeler içinde, tekrarlanabilir şekilde test edilir.
- (+) iOS geliştirici menüsünden tüm senaryolar elle oynanabilir.
- (−) `Instant.now()` kullanan tek bir satır testleri sessizce bozar. ArchUnit kuralı ile yasaklanır.
