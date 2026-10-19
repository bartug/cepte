# Java'dan Swift'e

iOS tarafını okurken takılmamak için notlar. Her kavram Java karşılığıyla ve projedeki gerçek bir dosyayla anlatılıyor.
Örnekler `ios/` altından.

## 1. Proje yapısı: Maven'ın karşılıkları

| Java / Maven | Swift / Xcode | Projede |
|---|---|---|
| `pom.xml` | `Package.swift` | `ios/Packages/Core/Package.swift` |
| Maven modülü | SPM paketi | `Core`, `DesignSystem`, `MiniAppKit`, `Features` |
| Modül içindeki paket (`com.x.wallet`) | **target** | `Networking`, `AppConfiguration`, `Home`, `DevMenu` |
| Artifact (jar) | **product** (library) | Uygulama `Home` product'ını kullanır |
| `<dependency>` | `dependencies: [.product(name:package:)]` | `Features` → `Core` |
| Spring Boot uygulaması | Xcode projesindeki **app target** | `ios/Cepte.xcodeproj` + `ios/Cepte/` |
| `mvn test` | `xcodebuild test` | `ios/scripts/test.sh` |

Bir target başka bir target'ı `dependencies`'te yazmadan `import` edemez. Modulith'in backend'de testle zorladığı
modül sınırını Swift derleyicinin kendisi zorlar: `Home` target'ı `DevMenu`'yü import etmeye çalışırsa derlenmez.

**Erişim seviyeleri:** Swift'te varsayılan erişim `internal`, yani "aynı target içinden görünür". Java'daki
package-private'ın target'a genişletilmiş hali. Paketlerdeki tiplerin önünde `public` yazmasının sebebi bu;
`public` olmayan hiçbir şey paketin dışından görünmez.

## 2. Değişkenler ve optional

```swift
let name = "Cepte"        // final String name = "Cepte";  (tip çıkarımı)
var count = 0             // int count = 0;
var config: AppConfig?    // null olabilir. Java'da her referans null olabilir; Swift'te sadece ? olanlar
```

Swift'te `null` yerine `nil` var ve sadece optional tiplerde olabilir. `AppConfig` asla nil olamaz, `AppConfig?`
olabilir. NullPointerException derleme zamanında önlenir.

```swift
// AppConfigStore.requiresUpdate (ios/Packages/Core/Sources/AppConfiguration/AppConfigStore.swift)
guard let config,
      let current = SemanticVersion(appVersion) else {
    return false          // nil ise erken çık
}
// buradan sonra config ve current nil değil
```

| Swift | Java karşılığı |
|---|---|
| `if let x = optional { }` | `if (x != null) { }` |
| `guard let x else { return }` | `if (x == null) return;` sonra x kullanılır |
| `optional ?? varsayılan` | `Optional.ofNullable(x).orElse(varsayılan)` |
| `optional?.title` | `x == null ? null : x.getTitle()` |

## 3. struct ve class

Swift'in Java'da olmayan en önemli ayrımı bu.

| | `struct` | `class` |
|---|---|---|
| Tip | Değer tipi: atanınca **kopyalanır** | Referans tipi: Java'daki gibi |
| Java karşılığı | `record` (ama mutable olabilir) | Normal sınıf |
| Kalıtım | Yok | Var |
| Projede | `AppConfig`, `RemoteMiniApp`, `MiniAppEntry`, tüm `View`'lar | `AppConfigStore`, `HomeViewModel`, `URLSessionHTTPClient` |

Kural: **veri struct, durumu olan ve paylaşılan nesne class.** `AppConfig` bir değer; iki ekran aynı konfigürasyonu
alsa da biri diğerininkini değiştiremez. `AppConfigStore` ise uygulamada tek bir tane var ve herkes aynı nesneye bakıyor.

## 4. enum: Java'daki sealed interface + record

Swift enum'larının her durumu kendi verisini taşıyabilir (*associated value*):

```swift
// ios/Packages/Core/Sources/Networking/APIError.swift
public enum APIError: Error {
    case transport(URLError.Code)
    case server(status: Int, message: String?)
    case decoding(String)
    case emptyBody
}
```

Java 21 karşılığı:

```java
sealed interface ApiError permits Transport, Server, Decoding, EmptyBody {}
record Transport(URLError.Code code) implements ApiError {}
record Server(int status, String message) implements ApiError {}
```

`switch` tüm durumları kapsamak zorunda; yeni bir `case` eklenince onu ele almayan her `switch` derlenmez.
`LoadState` (`idle`, `loading`, `loaded(Value)`, `failed(String)`) aynı fikir: "yükleniyor ama hata da var" gibi tutarsız
bir durum tip seviyesinde imkânsız.

## 5. protocol ve generic

`protocol` = `interface`. `extension` ile default metot eklenir (Java'daki `default` metotlar).

```swift
// ios/Packages/Core/Sources/Networking/HTTPClient.swift
public protocol HTTPClient: Sendable {
    func send<Response>(_ request: APIRequest<Response>) async throws -> Response
}
```

`APIRequest<AppConfig>` cevabın tipini taşıdığı için `client.send(.get("/v1/app/config"))` çağrısının `AppConfig`
döneceğini derleyici bilir. Java'daki `ParameterizedTypeReference` ihtiyacı yok.

`any HTTPClient` "bu protokolü uygulayan herhangi bir nesne" demek (Java'da arayüz tipinde değişken). Testlerde
`FakeClient`, uygulamada `URLSessionHTTPClient` verilir; Spring'deki `@MockBean` mantığı ama elle.

## 6. Hata yönetimi

```swift
func load() async {
    do {
        let loaded = try await client.send(.get("/v1/app/config"))
        config = loaded
    } catch {
        errorMessage = error.localizedDescription
    }
}
```

- `throws` = metot exception fırlatabilir. Java'nın checked exception'ı gibi çağıran `try` yazmak zorunda,
  ama hangi tipi fırlattığı imzada yazmaz.
- `try` çağrının başına yazılır, `do/catch` = `try/catch`.

## 7. Eşzamanlılık: async/await, actor, Sendable

| Swift | Java karşılığı |
|---|---|
| `async` metot | `CompletableFuture` dönen metot, ama sıralı kod gibi yazılır |
| `await` | `.join()` ama thread bloklanmaz; virtual thread'deki gibi askıya alınır |
| `Task { ... }` | `executor.submit(...)` / `Thread.ofVirtual().start(...)` |
| `actor` | Tüm metotları `synchronized` olan sınıf; ama kilit yerine sıra ile |
| `@MainActor` | "Sadece UI thread'inde çalışır". Android'deki `runOnUiThread` zorunlu hale gelmiş gibi |
| `Sendable` | "Thread'ler arası paylaşılabilir" işareti. Java'da karşılığı yok |

Swift 6'nın en büyük farkı: **veri yarışı (data race) derleme zamanında yakalanır.** Bir thread'den diğerine
`Sendable` olmayan bir nesne geçirmeye çalışırsan kod derlenmez. Java'da bu hatalar ancak çalışma zamanında, şanslıysan
yakalanır.

Projede ekranların okuduğu her şey (`AppConfigStore`, ViewModel'ler) `@MainActor`. Ağ isteği `await` ile arka planda
yapılır, sonuç geldiğinde değer yine ana thread'de değişir; ekranı güncellemek için ayrıca thread değiştirmek gerekmez.

## 8. SwiftUI: ekran bir nesne değil, bir tarif

Spring MVC'de Thymeleaf şablonunu düşün: veri değişince sayfa yeniden üretilir. SwiftUI de böyle çalışır, ama
saniyede 60 kez ve sadece değişen kısım için.

```swift
// ios/Packages/Features/Sources/Home/HomeView.swift (sadeleştirilmiş)
public struct HomeView: View {
    @State private var viewModel: HomeViewModel

    public var body: some View {
        ScrollView {
            Text(viewModel.greeting)
                .font(Typography.title)            // modifier: dekoratör zinciri gibi
            ForEach(viewModel.miniApps) { entry in
                MiniAppTile(entry: entry)
            }
        }
        .refreshable { await viewModel.refresh() } // aşağı çekince yenile
    }
}
```

- `View` bir **struct**; `body` ekranın o anki verisiyle nasıl görüneceğini anlatır. Ekranı elle güncelleyen bir
  `setText` çağrısı yok.
- `@Observable` sınıfın (`HomeViewModel`, `AppConfigStore`) bir özelliği değişince, o özelliği `body` içinde okuyan
  ekranlar yeniden çizilir. `HomeViewModel.miniApps` hesaplanan bir özellik; konfigürasyon yenilenince ızgara
  kendiliğinden güncellenir.
- `@State`: ekranın kendi durumu. View struct'ı sık sık yeniden oluşturulur, `@State` ile işaretlenen değer korunur.
- `$isPresented` → **Binding**: değişkenin kendisine okuma-yazma referansı. `sheet(isPresented: $isDevMenuPresented)`,
  sheet kapanınca değişkeni `false` yapabilsin diye.
- `.task { }`: ekran açılınca çalışan async iş; ekran kapanırsa otomatik iptal edilir.
- `#Preview`: Xcode'un sağındaki canvas'ta ekranı uygulamayı çalıştırmadan gösterir.

## 9. Uygulamanın açılışı (kodu okuma sırası)

```
CepteApp (@main)                         ios/Cepte/CepteApp.swift
  └─ AppDependencies                     ios/Cepte/AppDependencies.swift    ← Spring context'in elle kurulmuş hali
       HTTP istemcileri, AppConfigStore, MiniAppRegistry (Yemek, Taksi), HomeViewModel
  └─ RootView                            ios/Cepte/Shell/RootView.swift
       .task → configStore.load() → GET /v1/app/config
       config yok → açılış ekranı / hata | sürüm eski → güncelleme ekranı | → MainTabView
  └─ MainTabView                         ios/Cepte/Shell/MainTabView.swift
       5 tab; Ana Sayfa'da HomeView
       mini app'e dokununca → fullScreenCover → MiniAppContainerView → FoodMiniApp.makeEntryView
```

## 10. Testler: Swift Testing

```swift
@Test(arguments: [("1.0.0", "1.0.1", true), ("1.10.0", "1.9.0", false)])
func guncellemeZorunluluguSurumeGore(appVersion: String, minimum: String, expected: Bool) async {
    ...
    #expect(store.requiresUpdate(appVersion: appVersion) == expected)
}
```

| Swift Testing | JUnit 5 |
|---|---|
| `@Test` | `@Test` |
| `@Test(arguments: [...])` | `@ParameterizedTest` + `@MethodSource` |
| `#expect(a == b)` | `assertThat(a).isEqualTo(b)`; başarısız olunca iki tarafın değerini de yazar |
| `#expect(throws: X) { }` | `assertThatThrownBy(() -> ...)` |
| `@Suite(.serialized)` | Paralel çalışmayı kapatmak |

## 11. Xcode'da çalıştırma

1. `ios/Cepte.xcodeproj`'yi aç. Paketler sol panelde `Packages` altında görünür.
2. Üstte şema `Cepte`, cihaz `iPhone 17 Pro` seçili olsun. **⌘R** çalıştırır, **⌘U** testleri çalıştırır.
3. Backend ve external-sim çalışıyor olmalı (DEVOPS.md). Simülatördeki `localhost` Mac'in kendisi.
4. Bir paketteki ekranı tek başına görmek için dosyayı aç, sağdaki canvas'ta `#Preview` görünür (**⌥⌘↩**).
5. Geliştirici menüsü: Profil > Geliştirici menüsü ya da simülatörde **⌃⌘Z** (cihazı sallama).
