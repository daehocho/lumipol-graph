// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "LumipolGraphUI",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "LumipolGraphUI", targets: ["LumipolGraphUI"])
    ],
    dependencies: [
        .package(url: "https://github.com/pointfreeco/swift-snapshot-testing", from: "1.17.0")
    ],
    targets: [
        .binaryTarget(
            name: "LumipolGraph",
            path: "ios-renderer/Frameworks/LumipolGraph.xcframework"
        ),
        .target(
            name: "LumipolGraphUI",
            dependencies: ["LumipolGraph"],
            path: "ios-renderer/Sources/LumipolGraphUI"
        ),
        .testTarget(
            name: "LumipolGraphUITests",
            dependencies: [
                "LumipolGraphUI",
                "LumipolGraph",
                .product(name: "SnapshotTesting", package: "swift-snapshot-testing"),
            ],
            path: "ios-renderer/Tests/LumipolGraphUITests"
        ),
    ]
)
