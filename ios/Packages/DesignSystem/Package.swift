// swift-tools-version: 6.0
import PackageDescription

// Renk, tipografi, boşluk token'ları ve ortak bileşenler. Hiçbir pakete bağımlı değil.
let package = Package(
    name: "DesignSystem",
    platforms: [.iOS(.v18)],
    products: [
        .library(name: "DesignSystem", targets: ["DesignSystem"]),
    ],
    targets: [
        .target(name: "DesignSystem"),
        .testTarget(name: "DesignSystemTests", dependencies: ["DesignSystem"]),
    ]
)
