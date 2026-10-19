# ADR-005: iOS mimarisi

- **Durum:** Kabul edildi
- **Tarih:** 15.10.2026

## Bağlam

Uygulama bir süper uygulama: cüzdan ve finans çekirdek, yemek ve taksi mini app. Feature'lar bağımsız geliştirilebilmeli,
tek başına önizlenip test edilebilmeli. Swift 6 strict concurrency ile yazılacak.

## Karar

1. **SwiftUI**, minimum iOS 18.
2. **Swift Package Manager ile modülerleştirme:** `Core`, `DesignSystem`, `MiniAppKit` ve her feature ayrı paket.
   Feature'lar birbirine bağımlı olamaz.
3. **MVVM + `@Observable` + Router:** ViewModel'ler `@MainActor @Observable` sınıflar, navigasyon tab başına bir router
   ve tip güvenli `Route` enum'u ile `NavigationStack` üzerinden.
4. **Bağımlılıklar protokol ile** tanımlanır, SwiftUI `Environment` üzerinden verilir; testte ve preview'da mock takılır.
5. **Mini app sözleşmesi:** `MiniApp` protokolü (id, gereken KYC seviyesi, deeplink'ler, giriş ekranı).
   Mini app'ler `MiniAppContext` ile sadece izin verilen yetenekleri kullanır.
6. **API istemcisi** backend'in OpenAPI tanımından Swift OpenAPI Generator ile üretilir.
7. **Güvenlik:** token'lar Keychain'de, cihaz anahtarı Secure Enclave'de (CryptoKit), Face ID LocalAuthentication ile.

## Değerlendirilen alternatifler

| Alternatif | Neden seçilmedi |
|---|---|
| TCA (The Composable Architecture) | Güçlü ve test edilebilir, ama kendi kavramları (Reducer, Store, Effect, Dependency) Swift'i yeni öğrenirken ikinci bir öğrenme yükü. MVVM ile başlayıp gerekirse tek bir feature'da denenebilir. |
| UIKit | Yeni projede SwiftUI standart; UIKit sadece gereken yerde (`UIViewControllerRepresentable`). |
| Mini app'ler WebView | Finans uygulamasında güvenlik ve performans zayıflar. |
| Tek target, klasörlerle ayrım | Sınır yok, derleme yavaş, feature tek başına test edilemez. |

## Sonuçlar

- (+) Feature'lar tek başına derlenir, önizlenir ve test edilir.
- (+) Mini app eklemek, shell'e bir kayıt satırı eklemek demek.
- (−) Paket sayısı arttıkça `Package.swift` yönetimi gerekir.
- Xcode projesi XcodeGen/Tuist gibi bir üretici olmadan tutulur. Xcode 16'nın senkron klasörleri (`PBXFileSystemSynchronizedRootGroup`)
  sayesinde `ios/Cepte/` altına eklenen dosya proje dosyasına dokunmadan derlenir; kodun neredeyse tamamı zaten SPM paketlerinde.
