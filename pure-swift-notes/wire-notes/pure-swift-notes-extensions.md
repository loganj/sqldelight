# Wire Swift Extensions Implementation

This document details the implementation of Wire's extension system in Swift, covering both protocol buffer extensions and Wire-specific custom options.

## Core Extension System

### 1. Extension Registry

```swift
public final class ExtensionRegistry {
    // Singleton instance
    public static let shared = ExtensionRegistry()
    
    // Storage for extensions
    private var extensions: [
        String: [Int: ExtensionDescriptor]
    ] = [:]
    
    // Registration API
    public func register(
        _ descriptor: ExtensionDescriptor,
        for messageType: Message.Type
    ) {
        let typeName = messageType.protoMessageName
        extensions[typeName, default: [:]][
            descriptor.fieldNumber
        ] = descriptor
    }
    
    // Lookup API
    public func extension(
        forType messageType: Message.Type,
        fieldNumber: Int
    ) -> ExtensionDescriptor? {
        extensions[messageType.protoMessageName]?[fieldNumber]
    }
}
```

### 2. Extension Descriptor

```swift
public struct ExtensionDescriptor {
    // Core properties
    public let fieldNumber: Int
    public let name: String
    public let type: Any.Type
    public let isRepeated: Bool
    public let isMessage: Bool
    public let defaultValue: Any?
    
    // Wire format details
    public let wireType: WireType
    public let tag: UInt32
    
    // Extension scope
    public let containingType: Message.Type
    public let extendedType: Message.Type
}
```

## Wire Custom Options

### 1. Option Definition

```swift
public protocol WireOption: Codable {
    static var optionFieldNumber: Int { get }
    static var optionType: OptionType { get }
}

public enum OptionType {
    case file
    case message
    case field
    case enum
    case enumValue
    case service
    case method
}

// Example custom option
public struct JavaPackageOption: WireOption {
    public static let optionFieldNumber = 1
    public static let optionType = OptionType.file
    
    public let package: String
}
```

### 2. Option Registry

```swift
public final class OptionRegistry {
    public static let shared = OptionRegistry()
    
    private var options: [
        OptionType: [Int: WireOption.Type]
    ] = [:]
    
    public func register<T: WireOption>(_ option: T.Type) {
        options[T.optionType, default: [:]][
            T.optionFieldNumber
        ] = option
    }
    
    public func option(
        type: OptionType,
        fieldNumber: Int
    ) -> WireOption.Type? {
        options[type]?[fieldNumber]
    }
}
```

## Runtime Support

### 1. Dynamic Message Extension

```swift
extension Message {
    // Extension storage
    private var extensionValues: [Int: Any] = [:]
    
    // Extension access
    public func hasExtension<T: MessageExtension>(
        _ extension: T.Type
    ) -> Bool {
        extensionValues[T.fieldNumber] != nil
    }
    
    public func getExtension<T: MessageExtension>(
        _ extension: T.Type
    ) -> T? {
        extensionValues[T.fieldNumber] as? T
    }
    
    public mutating func setExtension<T: MessageExtension>(
        _ extension: T.Type,
        value: T
    ) {
        extensionValues[T.fieldNumber] = value
    }
}
```

### 2. Extension Serialization

```swift
extension Message {
    // Serialize extensions
    internal func serializeExtensions(
        to buffer: inout ProtoBuffer
    ) throws {
        for (number, value) in extensionValues {
            guard let descriptor = ExtensionRegistry.shared
                .extension(forType: Self.self, fieldNumber: number)
            else { continue }
            
            try buffer.writeTag(descriptor.tag)
            try buffer.writeExtensionValue(value, descriptor: descriptor)
        }
    }
    
    // Parse extensions
    internal mutating func parseExtension(
        _ number: Int,
        buffer: inout ProtoBuffer
    ) throws {
        guard let descriptor = ExtensionRegistry.shared
            .extension(forType: Self.self, fieldNumber: number)
        else {
            try buffer.skipField()
            return
        }
        
        let value = try buffer.readExtensionValue(descriptor: descriptor)
        extensionValues[number] = value
    }
}
```

## Code Generation Support

### 1. Extension Generator

```swift
struct ExtensionGenerator {
    func generateExtension(
        _ extension: ExtensionDescriptor
    ) -> String {
        """
        public extension \(extension.containingType) {
            enum \(extension.name): MessageExtension {
                public static let fieldNumber = \(extension.fieldNumber)
                public static let extendedType = \(extension.extendedType).self
                
                public static func get(
                    _ message: \(extension.extendedType)
                ) -> \(extension.type)? {
                    message.getExtension(Self.self)
                }
                
                public static func set(
                    _ value: \(extension.type),
                    on message: inout \(extension.extendedType)
                ) {
                    message.setExtension(Self.self, value: value)
                }
            }
        }
        """
    }
}
```

### 2. Option Generator

```swift
struct OptionGenerator {
    func generateOption(_ option: WireOption.Type) -> String {
        """
        public struct \(option)Option: WireOption {
            public static let optionFieldNumber = \(option.optionFieldNumber)
            public static let optionType = OptionType.\(option.optionType)
            
            private enum CodingKeys: String, CodingKey {
                case value
            }
            
            public let value: \(option)
            
            public init(_ value: \(option)) {
                self.value = value
            }
        }
        """
    }
}
```

## Usage Examples

### 1. Message Extensions

```swift
// Define extension
extension UserMessage {
    enum verificationStatus: MessageExtension {
        static let fieldNumber = 100
        static let extendedType = User.self
        
        public struct Value: Codable {
            public var isVerified: Bool
            public var verificationDate: Date?
        }
    }
}

// Use extension
var user = User()
user.setExtension(
    UserMessage.verificationStatus.self,
    value: .init(isVerified: true)
)
```

### 2. Custom Options

```swift
// Define custom option
public struct SwiftName: WireOption {
    public static let optionFieldNumber = 50001
    public static let optionType = OptionType.message
    
    public let name: String
}

// Use in proto
message User {
    option (wire.swift_name) = "AppUser";
    string name = 1;
}
```

## Future Considerations

1. **Type Safety**
   - Better compile-time checking
   - Improved type inference
   - Static validation

2. **Performance**
   - Optimized extension lookup
   - Cached descriptors
   - Better memory usage

3. **Tooling**
   - IDE support for extensions
   - Documentation generation
   - Migration tools