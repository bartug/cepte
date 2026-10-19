// swift-tools-version: 6.0
import PackageDescription

// Uygulamanın altyapısı. Feature paketleri buna bağımlıdır, Core hiçbir feature'ı bilmez.
let package = Package(
    name: "Core",
    platforms: [.iOS(.v18)],
    products: [
        .library(name: "Networking", targets: ["Networking"]),
        .library(name: "AppConfiguration", targets: ["AppConfiguration"]),
        .library(name: "Navigation", targets: ["Navigation"]),
    ],
    targets: [
        .target(name: "Networking"),
        .target(name: "AppConfiguration", dependencies: ["Networking"]),
        .target(name: "Navigation"),
        .testTarget(name: "NetworkingTests", dependencies: ["Networking"]),
        .testTarget(name: "AppConfigurationTests", dependencies: ["AppConfiguration", "Networking"]),
    ]
)
