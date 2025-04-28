# Wire Swift Serialization Architecture

This document details the serialization implementation for Wire's Swift protocol buffer support.

## Core Architecture

### 1. Binary Format

```swift
public struct BinarySerializer {
    private var buffer: ProtoBuffer
    
    // Wire format types
    private enum WireType: Int {
        case varint = 0
        case fixed64 = 1
        case lengthDelimited = 2
        case startGroup = 3
        case endGroup = 4
        case fixed32 = 5
    }
    
    // Field key encoding
    private func makeTag(fieldNumber: Int, wireType: WireType) -> UInt32 {
        return UInt32((fieldNumber << 3) | wireType.rawValue)
    }
}
```

### 2. Encoding Implementation

```swift
extension BinarySerializer {
    // Core encoding methods
    func encode<T: Message>(_ message: T) throws {
        try message.traverse { field in
            switch field {
            case .varint(let number, let value):
                try writeTag(number, .varint)
                try writeVarint(value)
                
            case .fixed64(let number, let value):
                try writeTag(number, .fixed64)
                try writeFixed64(value)
                
            case .lengthDelimited(let number, let data):
                try writeTag(number, .lengthDelimited)
                try writeBytes(data)
            }
        }
    }
    
    // Specialized encoding for different types
    private func writeVarint(_ value: UInt64) throws {
        var v = value
        while v >= 0x80 {
            try buffer.write(UInt8(v & 0x7f | 0x80))
            v >>= 7
        }
        try buffer.write(UInt8(v))
    }
}
```

### 3. Decoding Implementation

```swift
public struct BinaryDecoder {
    private let buffer: ProtoBuffer
    
    // Core decoding method
    func decode<T: Message>(_ type: T.Type) throws -> T {
        var message = T()
        while !buffer.isEmpty {
            let tag = try buffer.readVarint()
            let fieldNumber = Int(tag >> 3)
            let wireType = WireType(rawValue: Int(tag & 0x7))!
            
            try message.decodeField(
                number: fieldNumber,
                wireType: wireType,
                from: buffer
            )
        }
        return message
    }
}
```

## JSON Support

### 1. JSON Encoding

```swift
public struct JSONEncoder {
    private var json: [String: Any] = [:]
    
    func encode<T: Message>(_ message: T) throws -> Data {
        try message.traverse { field in
            switch field {
            case .scalar(let name, let value):
                json[name] = value
                
            case .message(let name, let msg):
                json[name] = try encode(msg)
                
            case .repeated(let name, let values):
                json[name] = try values.map { try encode($0) }
            }
        }
        return try JSONSerialization.data(
            withJSONObject: json,
            options: .prettyPrinted
        )
    }
}
```

### 2. JSON Decoding

```swift
public struct JSONDecoder {
    func decode<T: Message>(_ type: T.Type, from data: Data) throws -> T {
        let json = try JSONSerialization.jsonObject(
            with: data,
            options: []
        ) as! [String: Any]
        
        var message = T()
        try message.traverse { field in
            guard let value = json[field.name] else { return }
            try message.decodeJSON(field: field, value: value)
        }
        return message
    }
}
```

## Performance Optimizations

### 1. Buffer Management

```swift
public final class ProtoBuffer {
    // Efficient buffer management
    private var chunks: [UnsafeMutableBufferPointer<UInt8>]
    private var currentChunk: Int
    private let chunkSize: Int
    
    // Chunk allocation strategy
    private func allocateChunk() {
        let chunk = UnsafeMutableBufferPointer<UInt8>
            .allocate(capacity: chunkSize)
        chunks.append(chunk)
        currentChunk += 1
    }
    
    // Efficient writing
    func write(_ bytes: UnsafeRawBufferPointer) throws {
        if remainingSpace < bytes.count {
            allocateChunk()
        }
        chunks[currentChunk].baseAddress!
            .advanced(by: position)
            .copyMemory(from: bytes.baseAddress!, byteCount: bytes.count)
        position += bytes.count
    }
}
```

### 2. Zero-Copy Reading

```swift
public struct ZeroCopyReader {
    private let data: Data
    private var position: Int
    
    // Efficient string reading
    func readString() throws -> String {
        let length = try readVarint()
        let start = position
        position += length
        
        return data[start..<position].withUnsafeBytes { buffer in
            String(decoding: buffer, as: UTF8.self)
        }
    }
    
    // Efficient bytes reading
    func readBytes() throws -> Data {
        let length = try readVarint()
        let start = position
        position += length
        return data[start..<position]
    }
}
```

## Unknown Fields Handling

```swift
public struct UnknownFieldSet {
    private var fields: [Int: UnknownField]
    
    struct UnknownField {
        var varints: [UInt64]
        var fixed32s: [UInt32]
        var fixed64s: [UInt64]
        var lengthDelimited: [Data]
    }
    
    mutating func addVarint(
        fieldNumber: Int,
        value: UInt64
    ) {
        fields[fieldNumber, default: UnknownField()]
            .varints
            .append(value)
    }
}
```

## Extension Support

```swift
public protocol MessageExtension {
    static var extensionRange: ClosedRange<Int> { get }
    static var wireType: WireType { get }
}

extension Message {
    // Extension handling during serialization
    func serializeExtensions() throws -> Data {
        var buffer = ProtoBuffer()
        for (number, value) in extensions {
            guard let ext = extensionRegistry[number] else {
                continue
            }
            try buffer.writeTag(number, ext.wireType)
            try buffer.writeValue(value, as: ext.type)
        }
        return buffer.data
    }
}
```

## Future Enhancements

1. **Streaming Support**
   - Incremental parsing
   - Stream-based APIs
   - Chunked processing

2. **Schema Evolution**
   - Better unknown field handling
   - Field presence tracking
   - Migration support

3. **Performance Improvements**
   - SIMD operations
   - Memory pooling
   - Better buffer management