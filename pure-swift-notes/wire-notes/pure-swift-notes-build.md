# Wire Swift Build System Integration

This document outlines the build system integration strategy for Wire's Swift implementation.

## Build System Goals

1. **Seamless Integration**
   - Support for SwiftPM and CocoaPods
   - Xcode integration
   - Command line support

2. **Performance**
   - Incremental builds
   - Parallel processing
   - Efficient caching

3. **Developer Experience**
   - Clear error messages
   - Source maps support
   - IDE integration

## SwiftPM Integration

### 1. Plugin Definition

```swift
// Package.swift
import PackageDescription

let package = Package(
    name: "WireSwift",
    products: [
        .library(name: "WireSwift", targets: ["WireSwift"]),
        .plugin(name: "WireSwiftPlugin", targets: ["WireSwiftPlugin"])
    ],
    targets: [
        .plugin(
            name: "WireSwiftPlugin",
            capability: .buildTool(),
            dependencies: ["wire-swift-compiler"]
        ),
        .binaryTarget(
            name: "wire-swift-compiler",
            url: "https://github.com/square/wire/releases/download/v1.0.0/wire-swift-compiler.zip",
            checksum: "..."
        )
    ]
)
```

### 2. Plugin Implementation

```swift
import PackagePlugin

@main
struct WireSwiftPlugin: BuildToolPlugin {
    func createBuildCommands(
        context: PluginContext,
        target: Target
    ) async throws -> [Command] {
        let inputFiles = target.directory
            .filter { $0.extension == "proto" }
        
        let outputDir = context.pluginWorkDirectory
            .appending("Generated")
        
        return [
            .buildCommand(
                displayName: "Generating Wire Swift Code",
                executable: try context.tool(named: "wire-swift-compiler").path,
                arguments: buildArguments(
                    inputFiles: inputFiles,
                    outputDir: outputDir
                ),
                inputFiles: inputFiles,
                outputFiles: [outputDir]
            )
        ]
    }
}
```

## Xcode Integration

### 1. Build Phase Script

```bash
#!/bin/bash
set -e

# Configuration
WIRE_COMPILER="$BUILT_PRODUCTS_DIR/wire-swift-compiler"
PROTO_FILES="$SRCROOT/**/*.proto"
OUTPUT_DIR="$BUILT_PRODUCTS_DIR/Generated"

# Ensure output directory exists
mkdir -p "$OUTPUT_DIR"

# Generate code
"$WIRE_COMPILER" \
    --proto_path="$SRCROOT" \
    --swift_out="$OUTPUT_DIR" \
    $PROTO_FILES

# Add output to sources
find "$OUTPUT_DIR" -name "*.swift" -exec \
    echo {}"