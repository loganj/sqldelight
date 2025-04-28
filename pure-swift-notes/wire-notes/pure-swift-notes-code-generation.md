# Wire Swift Code Generation Architecture

This document outlines the architecture and implementation details for Wire's Swift code generation system.

## Code Generation Pipeline

```
[.proto files] → [Parser] → [IR] → [Swift Generator] → [Generated Swift Code]
```

### 1. Parsing Phase
- Reuse existing Wire proto parser
- Generate language-agnostic IR
- Preserve all proto file metadata
- Handle Wire-specific extensions

### 2. Intermediate Representation (IR)

```swift
struct MessageIR {
    let name: String
    let fields: [FieldIR]
    let oneofs: [OneofIR]
    let options: [OptionIR]
    let nestedTypes: [TypeIR]
    let extensions: [ExtensionIR]
}

struct FieldIR {
    let number: Int
    let name: String
    let type: TypeIR
    let label: Label // required, optional, repeated
    let options: [OptionIR]
}

enum TypeIR {
    case scalar(ScalarType)
    case message(String)
    case enum(String)
    case map(KeyType, ValueType)
}
```

### 3. Swift Code Generation

#### Message Generation
```swift
// Generated code structure
public struct {MessageName} {
    // Fields
    private var _requiredFields: Set<Int>
    private var _values: [Int: Any]
    
    // Generated accessors
    public var field: FieldType {
        get { ... }
        set { ... }
    }
    
    // Serialization
    public func serializedData() throws -> Data
    public init(serializedData: Data) throws
}
```

#### Builder Pattern (Optional)
```swift
public extension {MessageName} {
    struct Builder {
        private var message: {MessageName}
        
        public func setField(_ value: FieldType) -> Builder
        public func build() throws -> {MessageName}
    }
}
```

### 4. Generated File Structure

```swift
// Generated/{PackageName}/{MessageName}.swift

// 1. Imports
import Foundation
import WireRuntime

// 2. Message Definition
public struct UserMessage {
    // ... message implementation
}

// 3. Extensions
extension UserMessage: Message { ... }
extension UserMessage: Hashable { ... }
extension UserMessage: Codable { ... }

// 4. Nested Types
extension UserMessage {
    public enum Status: Int32 { ... }
    public struct Address { ... }
}
```

## Code Generation Features

### 1. Swift-Specific Optimizations

- Use of value types (structs) for messages
- Proper access control (public/internal/private)
- Optimized memory layout
- Swift-native collection types

### 2. Type Safety Features

```swift
// Generated type-safe accessors
public var id: Int64 {
    get { _getValue(1) ?? 0 }
    set { _setValue(1, newValue) }
}

// Type-safe enums
public enum UserType: Int32, ProtoEnum {
    case unknown = 0
    case regular = 1
    case admin = 2
}
```

### 3. Protocol Conformances

```swift
// Automatic protocol conformances
extension GeneratedMessage: Hashable {
    public func hash(into hasher: inout Hasher)
    public static func ==(lhs: Self, rhs: Self) -> Bool
}

extension GeneratedMessage: Codable {
    public func encode(to encoder: Encoder) throws
    public init(from decoder: Decoder) throws
}
```

## Build Integration

### 1. SwiftPM Plugin

```swift
// package.swift
.plugin(
    name: "WireSwiftPlugin",
    capability: .buildTool(),
    dependencies: ["wire-swift-compiler"]
)
```

### 2. Xcode Integration

```swift
// Build phases
1. Run wire-swift-compiler
2. Compile generated sources
3. Compile main sources
```

## Performance Considerations

### 1. Generated Code Optimization

- Minimize runtime overhead
- Efficient memory layout
- Lazy initialization where beneficial
- Optimized repeated field handling

### 2. Build Performance

- Incremental generation
- Parallel processing
- Caching of intermediate results
- Minimal regeneration triggers

## Testing Strategy

### 1. Generated Code Tests

```swift
final class GeneratedCodeTests: XCTestCase {
    func testSerialization() throws {
        let message = TestMessage()
        let data = try message.serializedData()
        let decoded = try TestMessage(serializedData: data)
        XCTAssertEqual(message, decoded)
    }
}
```

### 2. Generator Tests

```swift
final class GeneratorTests: XCTestCase {
    func testMessageGeneration() throws {
        let input = """
        message Test {
            required string name = 1;
        }
        """
        let output = try generator.generate(input)
        XCTAssertContains(output, "public var name: String")
    }
}
```

## Future Enhancements

1. **Source Locations**
   - Better error messages
   - IDE integration
   - Documentation generation

2. **Performance Optimization**
   - Profile-guided optimization
   - Custom allocators
   - Lazy parsing improvements

3. **Developer Experience**
   - Better debugging support
   - More detailed documentation
   - Migration tools