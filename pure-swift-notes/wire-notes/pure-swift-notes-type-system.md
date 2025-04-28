# Wire Swift Type System Implementation

This document details the implementation plan for Wire's Swift type system, focusing on Protocol Buffer type mapping and Swift-specific considerations.

## Protocol Buffer Type Mapping

### Scalar Types

| Proto Type | Swift Type | Notes |
|------------|------------|-------|
| double     | Double     | 64-bit floating point |
| float      | Float      | 32-bit floating point |
| int32      | Int32     | Uses NSNumber when optional |
| int64      | Int64     | Uses NSNumber when optional |
| uint32     | UInt32    | Uses NSNumber when optional |
| uint64     | UInt64    | Uses NSNumber when optional |
| sint32     | Int32     | ZigZag encoded |
| sint64     | Int64     | ZigZag encoded |
| fixed32    | UInt32    | Always 4 bytes |
| fixed64    | UInt64    | Always 8 bytes |
| sfixed32   | Int32     | Always 4 bytes |
| sfixed64   | Int64     | Always 8 bytes |
| bool       | Bool      | Uses NSNumber when optional |
| string     | String    | UTF-8 encoded |
| bytes      | Data      | Raw bytes |

### Message Types

```swift
public struct GeneratedMessage {
    // Base protocol for all generated messages
    public protocol Message: Sendable {
        // Core requirements
        static var protoMessageName: String { get }
        func serializedData() throws -> Data
        
        // Lifecycle
        init()
        init(serializedData: Data) throws
        
        // Wire-specific features
        var unknownFields: UnknownFieldSet { get }
    }
}
```

### Handling Optionals

#### Proto2
```swift
public struct Proto2Message {
    // Required fields
    private var _name: String
    public var name: String {
        get { _name }
        set { _name = newValue }
    }
    
    // Optional fields
    private var _age: Int32?
    public var hasAge: Bool { _age != nil }
    public var age: Int32 {
        get { _age ?? 0 }
        set { _age = newValue }
    }
}
```

#### Proto3
```swift
public struct Proto3Message {
    // All fields are implicitly optional
    public var name: String = ""
    public var age: Int32 = 0
    
    // Custom presence tracking if needed
    private var _presentFields = Set<Int>()
    public func hasField(_ number: Int) -> Bool {
        return _presentFields.contains(number)
    }
}
```

### Oneof Implementation

```swift
public enum UserType {
    case individual(Individual)
    case business(Business)
    case notSet
    
    // Swift-specific additions
    public var isIndividual: Bool {
        if case .individual = self { return true }
        return false
    }
    
    public var individual: Individual? {
        if case .individual(let value) = self { return value }
        return nil
    }
}
```

## Type Extensions

### Custom Options Support
```swift
public protocol WireOption {
    static var wireOptionName: String { get }
    static var wireOptionNumber: Int { get }
}

public protocol WireExtensible {
    func getExtension<T: WireOption>(_ type: T.Type) -> T?
    mutating func setExtension<T: WireOption>(_ value: T)
}
```

### Type Registry

```swift
public final class TypeRegistry {
    public static let shared = TypeRegistry()
    
    private var types: [String: Message.Type]
    
    public func register(_ type: Message.Type) {
        types[type.protoMessageName] = type
    }
    
    public func messageType(name: String) -> Message.Type? {
        return types[name]
    }
}
```

## Implementation Considerations

### Performance
1. **Value Types**
   - Messages are structs for better value semantics
   - Copy-on-write for large messages
   - Lazy parsing for better initialization performance

2. **Memory Layout**
   - Careful consideration of padding and alignment
   - Optimized storage for repeated fields
   - Efficient handling of strings and bytes

3. **Codegen Optimization**
   - Generated code should be as direct as possible
   - Minimize protocol witness tables
   - Reduce dynamic dispatch where possible

### Swift Idioms
1. **Property Wrappers**
   - Consider for field presence tracking
   - Potential for custom serialization behaviors
   - Validation and transformation support

2. **Result Builders**
   - Potential for message building DSL
   - More natural syntax for complex messages

3. **Codable Integration**
   - Support for Swift's Codable protocol
   - Custom coding keys for wire format

## Future Considerations

1. **Swift Concurrency**
   - Actor-based message handling
   - Async serialization/deserialization
   - Thread safety guarantees

2. **Property Wrappers Evolution**
   - More sophisticated presence tracking
   - Custom validation rules
   - Transform operations

3. **Swift ABI Stability**
   - Impact on binary size
   - Performance implications
   - Version compatibility