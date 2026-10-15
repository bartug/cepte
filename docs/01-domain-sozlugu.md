# Cüzdan ve Kredi Domain Sözlüğü

Projede geçen kavramları topladığım yer. payment-switch'te paranın POS'tan bankaya nasıl gittiğine bakmıştık; burada
paranın müşterinin cebinde nasıl durduğuna, nasıl hareket ettiğine ve kredinin nasıl verilip geri toplandığına bakıyoruz.

> Mevzuattaki oran ve süreler sık değişiyor. Bu dokümandaki rakamlar kavramı anlatmak için; projede hepsi
> konfigürasyondan okunur, koda gömülmez.

## 1. Taraflar

| Kavram | Açıklama |
|---|---|
| **Kullanıcı (User)** | Uygulamaya giriş yapan hesap. Telefon numarası, cihazlar, oturumlar. Kimliği bilinmeyebilir. |
| **Müşteri (Customer)** | Hukuki kişi. TCKN, ad soyad, doğum tarihi, adres. KYC'den geçen ve hakkında karar verilen taraf. |
| **Elektronik para kuruluşu (EPK)** | TCMB lisansıyla e-para ihraç eden ve cüzdan hesabı tutan kuruluş (6493 sayılı kanun). Cepte'nin cüzdanı bu lisansla çalışıyormuş gibi kurgulanır. |
| **Anlaşmalı banka (Lender)** | Krediyi asıl veren, bilançosunda tutan ve riskini taşıyan banka (*lender of record*). Projede **Banka A** ve **Banka B**. |
| **NVİ / KPS** | Nüfus ve Vatandaşlık İşleri. Kimlik bilgilerinin doğruluğu Kimlik Paylaşım Sistemi'nden teyit edilir. |
| **MASAK** | Mali Suçları Araştırma Kurulu. Suç gelirlerinin aklanması ve terörün finansmanıyla mücadele. Yaptırım listeleri ve şüpheli işlem bildirimi. |
| **KKB / Findeks** | Kredi Kayıt Bürosu. Kişinin kredi geçmişi ve kredi skoru. Sorgu için müşterinin izni gerekir. |
| **BDDK / TCMB** | Bankacılık ve ödeme sistemleri düzenleyicileri. Kredi BDDK'nın, ödeme ve e-para TCMB'nin alanı. |
| **Ödeme kuruluşu (PSP)** | Kartla para yüklemede kart işlemini yapan taraf. 3DS doğrulaması onun üzerinden yürür. |
| **Dikey (Vertical)** | Süper uygulamanın içindeki iş kolu: yemek, taksi, finans. |
| **Üye işyeri (Merchant)** | Dikeylerde ödeme alan taraf: restoran, taksi sürücüsü. |

## 2. Kimlik ve KYC

| Kavram | Açıklama |
|---|---|
| **KYC (Know Your Customer)** | Müşterinin kim olduğunu doğrulama süreci. Kimlik tespiti + risk değerlendirmesi. |
| **KYC seviyesi** | Doğrulamanın derinliği. Seviye arttıkça açılan özellikler ve limitler artar. Aşağıdaki tablo. |
| **Uzaktan kimlik tespiti** | Şubeye gitmeden müşteri edinimi: kimlik kartı çipini NFC ile okuma + canlılık testi (+ gerekirse görüntülü görüşme). |
| **NFC çip okuma** | Yeni kimlik kartının çipinde fotoğraf ve kimlik bilgisi imzalı olarak durur. Çipe erişim için kartın üzerindeki MRZ bilgisiyle anahtar türetilir (BAC/PACE). İmza doğrulanınca bilgilerin değiştirilmediği kanıtlanır. |
| **Canlılık testi (Liveness)** | Kameradaki yüzün gerçek ve canlı olduğunu (fotoğraf, maske, video değil) ve çipteki fotoğrafla eşleştiğini doğrular. |
| **PEP** | Politically Exposed Person. Siyasi nüfuz sahibi kişiler; ek inceleme gerektirir. |
| **Yaptırım taraması (Screening)** | Müşterinin MASAK ve uluslararası yaptırım listelerinde olup olmadığının kontrolü. İsim benzerliği manuel incelemeye düşer. |
| **Manuel inceleme** | Otomatik karar verilemeyen durumların operasyon ekibine düşmesi. |

### KYC seviyeleri

| Seviye | Doğrulanan | Açılan |
|---|---|---|
| **0** | Telefon (OTP) | Yemek ve taksi siparişi, kartla ödeme. Cüzdan yok. |
| **1** | TCKN + ad soyad + doğum tarihi NVİ'den teyitli, yaptırım taraması temiz | Düşük limitli cüzdan, sanal IBAN, para yükleme ve gönderme |
| **2** | Uzaktan kimlik tespiti (NFC + canlılık), adres | Yüksek limitli cüzdan, IBAN'a para çekme, **kredi değerlendirmesi** |

## 3. Cüzdan

| Kavram | Açıklama |
|---|---|
| **Elektronik para (e-para)** | Karşılığında fon alınarak ihraç edilen ve ödeme için kullanılan değer. Cüzdandaki bakiye hukuken e-paradır, mevduat değildir; faiz işlemez. |
| **Fon koruma (Safeguarding)** | Müşterilerin e-para karşılığı fonları şirketin kendi parasından ayrı, korunan hesaplarda tutulur. Şirket batsa bile müşteri parası güvencededir. |
| **Toplam bakiye** | Hesaptaki tüm para. |
| **Bloke (Hold)** | Henüz tahsil edilmemiş ama ayrılmış tutar. Taksi yolculuğu başlarken tahmini tutar bloke edilir. |
| **Kullanılabilir bakiye** | Toplam bakiye − bloke. Harcamalar buna göre kontrol edilir. |
| **Sanal IBAN** | Müşteriye özel IBAN. Buraya gelen havale/FAST otomatik olarak o müşterinin cüzdanına düşer, açıklama okumaya gerek kalmaz. |
| **Limit** | KYC seviyesine göre günlük/aylık para yükleme, gönderme ve bakiye üst sınırı. |
| **Hesap dondurma** | Şüpheli işlem, yaptırım eşleşmesi ya da müşteri talebiyle hesaptan para çıkışının durdurulması. |

## 4. Ledger (çift taraflı muhasebe)

| Kavram | Açıklama |
|---|---|
| **Hesap (Ledger account)** | Paranın durduğu her yer bir hesaptır: müşterinin cüzdanı, şirketin fon hesabı, restoranın alacağı, bankaya borcumuz. |
| **Yevmiye kaydı (Journal entry)** | Bir para hareketi. Değiştirilemez, sadece ters kayıtla düzeltilir. |
| **Posting** | Yevmiye kaydının satırları. Her kayıtta borç (D) ve alacak (C) toplamı eşit olmak zorunda. |
| **Normal yön** | Varlık hesapları borç yönünde, yükümlülük hesapları alacak yönünde artar. Müşteri cüzdanı bizim için bir **yükümlülüktür** (müşteriye borçluyuz). |
| **Değer tarihi (Value date)** | Paranın hukuken hareket ettiği tarih. Kayıt zamanından farklı olabilir: gece 23:59'da yapılan ödeme ertesi gün kaydedilse de o günün tarihini taşır. |

## 5. Ödeme ve transfer

| Kavram | Açıklama |
|---|---|
| **Ödeme niyeti (Payment intent)** | Bir dikeyin "şu sipariş için şu tutarı tahsil et" isteği. Ödeme yöntemi sonradan seçilir; deneme başarısız olursa aynı niyet farklı yöntemle tekrar denenir. |
| **Ön provizyon / tahsil (Pre-auth / Capture)** | Tutarı önce ayırmak, sonra gerçek tutarı tahsil etmek. Taksi örneği. |
| **İptal / İade (Void / Refund)** | Tahsil edilmemiş blokajı bırakmak / tahsil edilmiş parayı geri ödemek. |
| **3D Secure (3DS)** | Kartla ödemede kart sahibinin bankası tarafından ek doğrulama (SMS kodu, banka uygulaması onayı). |
| **Kart tokenizasyonu** | Kart numarası bizde tutulmaz. PSP kartı saklar, bize bir token verir; sonraki yüklemeler token ile yapılır. |
| **P2P transfer** | Cüzdandan cüzdana, telefon numarasıyla. Ledger içinde anında gerçekleşir. |
| **FAST** | TCMB'nin 7/24 anlık ödeme sistemi. Belirli tutarın altındaki IBAN transferleri saniyeler içinde gerçekleşir. |
| **EFT / Havale** | Bankalar arası transfer (EFT) / aynı banka içi transfer (havale). EFT mesai saatlerine bağlıdır. |
| **TR Karekod** | TCMB'nin standart QR ödeme formatı. Üye işyeri QR'ı okutularak ödeme yapılır. |
| **Step-up authentication** | Riskli bir işlemde (yüksek tutar, yeni alıcı) kullanıcıdan ek doğrulama (Face ID / PIN) istenmesi. |

## 6. Kredi

| Kavram | Açıklama |
|---|---|
| **Gömülü finans (Embedded finance)** | Finansal ürünün finans dışı bir uygulamanın içinde sunulması. Krediyi banka verir, müşteri deneyimi bizde. |
| **Değerlendirme (Assessment)** | Müşteri hakkında verilen kredi kararı: kesin kurallar + skor + politika matrisi. Girdileriyle birlikte değiştirilemez saklanır. |
| **İnce dosya (Thin file)** | Kredi geçmişi olmayan ya da çok az olan müşteri. KKB skoru anlamsızdır; iç veri (sipariş geçmişi) burada değer kazanır. |
| **Teklif (Offer)** | Değerlendirme sonucu müşteriye sunulan limit, faiz ve vade. Süreli; süresi dolarsa yeniden değerlendirme gerekir. |
| **Başvuru (Application)** | Müşterinin bir teklifi kabul edip bankaya iletilmesi. Banka son kararı verir. |
| **Kredi limiti (Credit line)** | Bankanın müşteriye tanıdığı, tekrar tekrar kullanılabilen üst sınır. Ödendikçe limit tekrar açılır (revolving). |
| **Kullanım (Drawdown)** | Limitten yapılan her kullanım. Kendi tutarı, vadesi ve ödeme planı vardır. |
| **BNPL (Buy Now Pay Later)** | "Şimdi al, sonra öde". Checkout'ta limitten yapılan kullanım. |
| **Nakit kullanım** | Limitten cüzdana para aktarılması. Klasik ihtiyaç kredisinin karşılığı. |
| **Akdi faiz** | Sözleşmede belirlenen faiz oranı (aylık). |
| **KKDF / BSMV** | Tüketici kredisinde faiz tutarı üzerinden alınan vergi ve fon. Taksit tutarı bunları içerir. Projede örnek olarak %15 + %15. |
| **Yıllık maliyet oranı (YMO)** | Faiz, vergi ve masrafların hepsi dahil yıllık maliyet. Müşteriye gösterilmesi zorunlu. |
| **Ödeme planı** | Taksitlerin vade tarihleri ve her taksitteki anapara, faiz, vergi dağılımı. |
| **Eşit taksit (Annuity)** | Her ay aynı tutarın ödendiği plan. İlk taksitlerde faiz payı yüksek, son taksitlerde anapara payı yüksektir. |
| **Ön bilgilendirme formu** | Sözleşmeden önce müşteriye verilmesi zorunlu, kredinin tüm maliyetini gösteren form. |
| **Cayma hakkı** | Tüketicinin sözleşmeden itibaren 14 gün içinde gerekçesiz cayabilmesi. |
| **Erken kapama** | Borcun vadeden önce ödenmesi. Erken ödenen kısım için faiz indirimi yapılır; tutarı banka hesaplar. |

### Eşit taksit hesabı

Vergiler faiz üzerinden alındığı için hesapta brüt oran kullanılır:

```
r_brüt = r_akdi × (1 + KKDF + BSMV)          örn: 0,0349 × 1,30 = 0,04537
taksit = P × r_brüt / (1 − (1 + r_brüt)^−n)

Her ay:  faiz    = kalan anapara × r_akdi
         KKDF    = faiz × KKDF oranı
         BSMV    = faiz × BSMV oranı
         anapara = taksit − faiz − KKDF − BSMV
```

Kuruş yuvarlaması her satırda yapılır; biriken fark **son taksite** eklenir. Aksi halde taksitlerin anapara toplamı
kullanılan tutarı tutmaz.

## 7. Geri ödeme ve tahsilat

| Kavram | Açıklama |
|---|---|
| **Vade günü (Due date)** | Taksitin ödenmesi gereken gün. 29-31 arası günler kullanılmaz (şubatta yok). |
| **Otomatik ödeme talimatı (Autopay mandate)** | Müşterinin "vade günü taksiti cüzdanımdan çek" izni. Tarihli, iptal edilebilir. |
| **Tahsilat denemesi (Collection attempt)** | Vade günü cüzdandan çekme denemesi. Yetersiz bakiyede gün içinde tekrarlanır. |
| **Tahsilat aracısı** | Bankanın adına müşteriden parayı toplayan taraf (biz). Cüzdandan çekildiği an müşteri ödemiş sayılır; bankaya aktarım bizim sorumluluğumuzdur. |
| **Ödeme dağıtım sırası (Waterfall)** | Gelen paranın hangi kalemleri hangi sırayla kapattığı: gecikme faizi → masraf → vergiler → faiz → anapara. |
| **Gecikme (Delinquency)** | Vade gününün ertesi günü başlar. **DPD (days past due)** ile ölçülür. |
| **Gecikme faizi (Temerrüt faizi)** | Geciken tutar için işleyen faiz. Tüketici kredisinde akdi faizin %30 fazlasını geçemez. |
| **Muacceliyet** | Kalan borcun tamamının bir anda istenebilir hale gelmesi. Tüketici kredisinde ardışık en az iki taksit ödenmemiş olmalı ve müşteriye en az 30 gün süre verilmeli. |
| **Takipteki alacak (NPL)** | 90 günü geçen gecikme. Banka karşılık ayırır, yasal takip başlar. |
| **Toplu aktarım (Settlement batch)** | Gün içinde tahsil edilen taksitlerin bankaya tek bir transferle gönderilmesi ve yanındaki mutabakat dosyası. |
| **Üç taraflı mutabakat** | Cüzdandan çekilen = bankaya bildirilen = bankaya aktarılan. |

### Gecikme aşamaları

| DPD | Aşama | Sistemde ne olur |
|---|---|---|
| −3 | Hatırlatma | Push: "Taksidin 3 gün sonra" |
| 0 | Vade günü | Otomatik tahsilat, gün içinde tekrarlar |
| 1–30 | Erken gecikme | Limit dondurulur, günlük hatırlatma, gecikme faizi işler (banka hesaplar) |
| 30+ | Muacceliyet riski | Ardışık 2 taksitte banka ihtar gönderir |
| 90+ | Takipteki alacak | Banka yasal takibe geçer; kredi limiti kapatılır |
