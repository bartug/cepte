// swift-tools-version: 6.0
import PackageDescription

// Her feature ayrı bir target. Feature'lar birbirine bağımlı olamaz; aralarındaki geçişi uygulama shell'i yapar.
let package = Package(
    name: "Features",
    platforms: [.iOS(.v18)],
    products: [
        .library(name: "Home", targets: ["Home"]),
        .library(name: "DevMenu", targets: ["DevMenu"]),
        .library(name: "FoodMiniApp", targets: ["FoodMiniApp"]),
        .library(name: "TaxiMiniApp", targets: ["TaxiMiniApp"]),
    ],
    dependencies: [
        .package(path: "../Core"),
        .package(path: "../DesignSystem"),
        .package(path: "../MiniAppKit"),
    ],
    targets: [
        .target(name: "Home", dependencies: [
            .product(name: "AppConfiguration", package: "Core"),
            .product(name: "DesignSystem", package: "DesignSystem"),
            .product(name: "MiniAppKit", package: "MiniAppKit"),
        ]),
        .target(name: "DevMenu", dependencies: [
            .product(name: "Networking", package: "Core"),
            .product(name: "AppConfiguration", package: "Core"),
            .product(name: "DesignSystem", package: "DesignSystem"),
        ]),
        .target(name: "FoodMiniApp", dependencies: [
            .product(name: "MiniAppKit", package: "MiniAppKit"),
            .product(name: "DesignSystem", package: "DesignSystem"),
        ]),
        .target(name: "TaxiMiniApp", dependencies: [
            .product(name: "MiniAppKit", package: "MiniAppKit"),
            .product(name: "DesignSystem", package: "DesignSystem"),
        ]),
        .testTarget(name: "HomeTests", dependencies: ["Home", .product(name: "Networking", package: "Core")]),
        .testTarget(name: "DevMenuTests", dependencies: ["DevMenu", .product(name: "Networking", package: "Core")]),
    ]
)
