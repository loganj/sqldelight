// swift-tools-version:5.5
import PackageDescription

let package = Package(
    name: "SQLDelight",
    platforms: [
        .iOS(.v13),
        .macOS(.v11)
    ],
    products: [
        .library(
            name: "SQLDelight",
            targets: ["SQLDelight"]
        ),
    ],
    dependencies: [
        // SQLite dependency for the test driver
        .package(url: "https://github.com/stephencelis/SQLite.swift.git", from: "0.14.1"),
    ],
    targets: [
        .target(
            name: "SQLDelight",
            dependencies: []
        ),
        .testTarget(
            name: "SQLDelightTests",
            dependencies: [
                "SQLDelight",
                .product(name: "SQLite", package: "SQLite.swift"),
            ]
        ),
    ]
)