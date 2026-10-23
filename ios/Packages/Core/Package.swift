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
        .library(name: "DeviceSecurity", targets: ["DeviceSecurity"]),
        .library(name: "SessionKit", targets: ["SessionKit"]),
    ],
    targets: [
        .target(name: "Networking"),
        .target(name: "AppConfiguration", dependencies: ["Networking"]),
        .target(name: "Navigation"),
        // Apple'ın Security framework'üyle karışmasın diye DeviceSecurity
        .target(name: "DeviceSecurity", dependencies: ["Networking"]),
        .target(name: "SessionKit", dependencies: ["Networking", "DeviceSecurity"]),
        .testTarget(name: "NetworkingTests", dependencies: ["Networking"]),
        .testTarget(name: "AppConfigurationTests", dependencies: ["AppConfiguration", "Networking"]),
        .testTarget(name: "DeviceSecurityTests", dependencies: ["DeviceSecurity", "Networking"]),
        .testTarget(name: "SessionKitTests", dependencies: ["SessionKit", "DeviceSecurity", "Networking"]),
    ]
)
