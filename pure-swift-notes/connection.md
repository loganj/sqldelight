# Connection Management

## Connection Pools

SQLDelight's native driver uses two connection pools:

```swift
actor DatabaseDriver {
    private let transactionPool: ConnectionPool<DatabaseConnection>
    private let readerPool: ConnectionPool<DatabaseConnection>
    
    init(maxReaderConnections: Int = 1) {
        // Single connection for transactions
        transactionPool = ConnectionPool(capacity: 1) {
            DatabaseConnection(readWrite: true)
        }
        
        // Configurable pool for readers
        readerPool = ConnectionPool(capacity: maxReaderConnections) {
            let conn = DatabaseConnection(readWrite: false)
            try? conn.execute("PRAGMA query_only = 1")
            return conn
        }
    }
}
```

### Reader Pool
- Multiple connections (configurable)
- Read-only operations
- Query execution outside transactions
- Connection reuse

### Transaction Pool
- Single connection
- All write operations
- Transaction handling
- Statement caching

## Connection Lifecycle

### Creation
```swift
actor ConnectionPool<T> {
    private let capacity: Int
    private let producer: () -> T
    private var connections: [Connection] = []
    
    struct Connection {
        let value: T
        var inUse: Bool
    }
    
    init(capacity: Int, producer: @escaping () -> T) {
        self.capacity = capacity
        self.producer = producer
    }
}
```

### Reuse
```swift
actor ConnectionPool<T> {
    func acquire() -> Query<T> {
        Query { [weak self] in
            guard let self = self else { return nil }
            
            // Try to find an available connection
            if let index = connections.firstIndex(where: { !$0.inUse }) {
                connections[index].inUse = true
                return connections[index].value
            }
            
            // Create new if under capacity
            if connections.count < capacity {
                let connection = Connection(value: producer(), inUse: true)
                connections.append(connection)
                return connection.value
            }
            
            return nil
        }
    }
    
    func release(_ connection: T) {
        if let index = connections.firstIndex(where: { $0.value === connection }) {
            connections[index].inUse = false
        }
    }
}
```

### Cleanup
```swift
actor DatabaseConnection {
    private var db: OpaquePointer?
    private var statements: [Int: Statement] = [:]
    
    func close() {
        statements.values.forEach { $0.finalize() }
        statements.removeAll()
        if let db = db {
            sqlite3_close(db)
            self.db = nil
        }
    }
}
```

## Transaction Management

### Transaction Tracking
```swift
actor DatabaseDriver {
    private var activeTransaction: Transaction?
    
    var currentTransaction: Transaction? {
        activeTransaction
    }
}
```

### Nesting Support
```swift
actor DatabaseDriver {
    func transaction<T>(_ block: (Transaction) throws -> Query<T>) -> Query<T> {
        Query { [weak self] in
            guard let self = self else { return nil }
            
            // If already in transaction, nest it
            if let transaction = activeTransaction {
                return try block(transaction).execute()
            }
            
            // Get connection from transaction pool
            guard let connection = try transactionPool.acquire().execute() else {
                return nil
            }
            defer { transactionPool.release(connection) }
            
            let transaction = Transaction(connection: connection)
            activeTransaction = transaction
            defer { activeTransaction = nil }
            
            try connection.execute("BEGIN TRANSACTION")
            do {
                let result = try block(transaction).execute()
                try connection.execute("COMMIT")
                return result
            } catch {
                try connection.execute("ROLLBACK")
                throw error
            }
        }
    }
}
```

### Cleanup
```swift
class Transaction {
    private var afterCommit: [() -> Void] = []
    private var afterRollback: [() -> Void] = []
    
    func afterCommit(_ block: @escaping () -> Void) {
        afterCommit.append(block)
    }
    
    func afterRollback(_ block: @escaping () -> Void) {
        afterRollback.append(block)
    }
    
    func notifyComplete() {
        afterCommit.forEach { $0() }
    }
    
    func notifyRollback() {
        afterRollback.forEach { $0() }
    }
}
```

## Statement Management

### Statement Caching
```swift
actor DatabaseConnection {
    private var statements: [Int: Statement] = [:]
    
    func prepare(_ sql: String, id: Int? = nil) throws -> Statement {
        if let id = id {
            // Return cached statement if available
            if let stmt = statements[id] {
                return stmt
            }
            // Create and cache new statement
            let stmt = try Statement(db: db, sql: sql)
            statements[id] = stmt
            return stmt
        }
        // Create non-cached statement
        return try Statement(db: db, sql: sql)
    }
}
```

### Statement Lifecycle
```swift
// Note: Statement itself is not an actor as it's only accessed through DatabaseConnection
final class Statement {
    private var stmt: OpaquePointer?
    
    init(db: OpaquePointer?, sql: String) throws {
        // Initialize statement
    }
    
    func bind(_ value: SQLiteValue, at index: Int32) throws {
        switch value {
        case .integer(let i): try bindInt(i, at: index)
        case .real(let d): try bindDouble(d, at: index)
        case .text(let s): try bindText(s, at: index)
        case .blob(let d): try bindBlob(d, at: index)
        case .null: try bindNull(at: index)
        }
    }
    
    func finalize() {
        if let stmt = stmt {
            sqlite3_finalize(stmt)
            self.stmt = nil
        }
    }
    
    deinit {
        finalize()
    }
}
```