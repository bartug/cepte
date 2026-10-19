// swift-tools-version: 6.0
import PackageDescription

// Süper uygulama ile mini uygulamalar arasındaki sözleşme. Mini app'ler sadece bu pakete ve DesignSystem'e bağlanır.
let package = Package(
    name: "MiniAppKit",
    platforms: [.iOS(.v18)],
    products: [
        .library(name: "MiniAppKit", targets: ["MiniAppKit"]),
    ],
    dependencies: [
        .package(path: "../Core"),
    ],
    targets: [
        .target(name: "MiniAppKit", dependencies: [.product(name: "AppConfiguration", package: "Core")]),
        .testTarget(name: "MiniAppKitTests", dependencies: ["MiniAppKit"]),
    ]
)
