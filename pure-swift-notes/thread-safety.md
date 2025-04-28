# Thread Safety

SQLite connections are not thread-safe - they must be accessed from a single thread/executor. Swift actors provide a natural way to enforce this requirement.

## Actor Protection

### Connection Safety
```swift
actor DatabaseDriver {
    private let transactionPool: ConnectionPool<DatabaseConnection>
    private let readerPool: ConnectionPool<DatabaseConnection>
    private var activeTransaction: Transaction?
    
    var currentTransaction: Transaction? {
        activeTransaction
    }
    
    // Connections are protected by the actor
    init(maxReaderConnections: Int = 1) {
        transactionPool = ConnectionPool(capacity: 1) {
            DatabaseConnection(readWrite: true)
        }
        
        readerPool = ConnectionPool(capacity: maxReaderConnections) {
            let conn = DatabaseConnection(readWrite: false)
            try? conn.execute("PRAGMA query_only = 1")
            return conn
        }
    }
}
```

### Statement Safety
```swift
actor DatabaseConnection {
    private var db: OpaquePointer?
    private var statements: [Int: Statement] = [:]
    
    // Statement access is protected by the actor
    func prepare(_ sql: String, id: Int? = nil) throws -> Statement {
        if let id = id {
            if let stmt = statements[id] {
                return stmt
            }
            let stmt = try Statement(db: db, sql: sql)
            statements[id] = stmt
            return stmt
        }
        return try Statement(db: db, sql: sql)
    }
}
```

### Pool Safety
```swift
actor ConnectionPool<T> {
    private var connections: [Connection] = []
    
    // Connection management is protected by the actor
    func acquire() -> Query<T> {
        Query { [weak self] in
            guard let self = self else { return nil }
            
            if let index = connections.firstIndex(where: { !$0.inUse }) {
                connections[index].inUse = true
                return connections[index].value
            }
            
            if connections.count < capacity {
                let connection = Connection(value: producer(), inUse: true)
                connections.append(connection)
                return connection.value
            }
            
            return nil
        }
    }
}
```

## Transaction Isolation

### Write Coordination
```swift
actor DatabaseDriver {
    func execute<T>(_ sql: String) -> Query<T> {
        Query { [weak self] in
            guard let self = self else { return nil }
            
            // Writes in transaction use transaction connection
            if let transaction = activeTransaction {
                return try transaction.execute(sql).execute()
            }
            
            // Other writes use writer pool
            guard let connection = try readerPool.acquire().execute() else {
                return nil
            }
            defer { readerPool.release(connection) }
            return try connection.execute(sql)
        }
    }
}
```

### Read/Write Separation
```swift
actor DatabaseDriver {
    // Single writer, multiple readers
    init(maxReaderConnections: Int = 1) {
        // Single connection for transactions/writes
        transactionPool = ConnectionPool(capacity: 1) {
            DatabaseConnection(readWrite: true)
        }
        
        // Multiple connections for reads
        readerPool = ConnectionPool(capacity: maxReaderConnections) {
            let conn = DatabaseConnection(readWrite: false)
            try? conn.execute("PRAGMA query_only = 1")
            return conn
        }
    }
}
```

## Implementation Strategy

1. **Thread Safety**
   - Actor-based protection
   - Isolated state
   - Connection coordination
   - Clear ownership

2. **Concurrency Model**
   - Single writer
   - Multiple readers
   - Transaction isolation
   - Actor boundaries

This implementation uses Swift actors to enforce SQLite's thread-safety requirements while maintaining SQLDelight's connection pooling and transaction management patterns.