# Core Types

## Query<T>

The core type for database operations:

```swift
struct Query<T> {
    private let impl: () throws -> T?
    
    init(_ impl: @escaping () throws -> T?) {
        self.impl = impl
    }
    
    func execute() throws -> T? {
        try impl()
    }
}
```

## Database Interface

The main database interface:

```swift
protocol Database {
    func query<T>(_ sql: String) -> Query<T>
    func transaction<T>(_ block: (Transaction) throws -> Query<T>) -> Query<T>
}

protocol Transaction {
    var connection: Connection { get }
    func afterCommit(_ block: @escaping () -> Void)
    func afterRollback(_ block: @escaping () -> Void)
}
```

## Type Adapters

Column adapters for custom type conversion:

```swift
protocol ColumnAdapter {
    associatedtype ModelType      // The Swift type (e.g., User, Date)
    associatedtype DatabaseType   // The SQLite type (Int, Double, String, Data)
    
    func decode(_ databaseValue: DatabaseType) -> ModelType
    func encode(_ value: ModelType) -> DatabaseType
}

// Example implementation
struct DateAdapter: ColumnAdapter {
    func decode(_ databaseValue: Int) -> Date {
        Date(timeIntervalSince1970: TimeInterval(databaseValue))
    }
    
    func encode(_ value: Date) -> Int {
        Int(value.timeIntervalSince1970)
    }
}
```

## Error Types

Standard error handling:

```swift
enum SQLiteError: Error {
    case connectionClosed
    case transactionActive
    case prepareFailed(code: Int32)
    case bindFailed(code: Int32)
    case executionFailed(code: Int32)
}
```

## Default Type Mappings

Standard SQLite to Swift type mappings:

```swift
// SQLite Type -> Swift Type
INTEGER -> Int
REAL -> Double
TEXT -> String
BLOB -> Data
NULL -> Optional<T>
```

## Implementation Strategy

1. **Type System Design**
   - Value types for models
   - Clear nullability
   - Type-safe interfaces
   - Minimal adapters

2. **Swift Integration**
   - Native types
   - Optional handling
   - Error handling
   - Clear ownership

3. **Error Handling**
   - Type-safe errors
   - Clear messages
   - Source tracking
   - Resource cleanup

4. **Resource Management**
   - Statement finalization
   - Connection cleanup
   - Pool management
   - Memory handling