import Foundation

/// Actor-protected database driver implementation
public actor DatabaseDriver {
    private let transactionPool: ConnectionPool<any DatabaseConnection>
    private let readerPool: ConnectionPool<any DatabaseConnection>
    private var activeTransaction: TransactionImpl?
    
    public var currentTransaction: Transaction? {
        activeTransaction
    }
    
    public init(maxReaderConnections: Int = 1, connectionFactory: @escaping () -> any DatabaseConnection) {
        transactionPool = ConnectionPool(capacity: 1, producer: connectionFactory)
        readerPool = ConnectionPool(capacity: maxReaderConnections, producer: connectionFactory)
    }
    
    public func execute<T>(_ sql: String, mapper: @escaping (SQLDelightStatement) throws -> T) async throws -> T {
        if let transaction = activeTransaction {
            let stmt = try await transaction.connection.prepare(sql)
            return try mapper(stmt)
        }
        
        guard let connection = try await readerPool.acquire() else {
            throw SQLiteError.connectionClosed
        }
        
        let stmt = try await connection.prepare(sql)
        defer { Task { await readerPool.release(connection) } }
        return try mapper(stmt)
    }
    
    public func transaction<T>(_ block: @escaping (Transaction) async throws -> T) async throws -> T {
        guard activeTransaction == nil else {
            throw SQLiteError.transactionActive
        }
        
        guard let connection = try await transactionPool.acquire() else {
            throw SQLiteError.connectionClosed
        }
        
        try await connection.beginTransaction()
        let transaction = TransactionImpl(connection: connection)
        activeTransaction = transaction
        
        do {
            let result = try await block(transaction)
            try await connection.commit()
            transaction.commit()
            return result
        } catch {
            try await connection.rollback()
            transaction.rollback()
            throw error
        }
    }
}

/// Actor-protected connection pool
actor ConnectionPool<T> {
    private struct Connection {
        let value: T
        var inUse: Bool
    }
    
    private let capacity: Int
    private let producer: () -> T
    private var connections: [Connection] = []
    
    init(capacity: Int, producer: @escaping () -> T) {
        self.capacity = capacity
        self.producer = producer
    }
    
    func acquire() async throws -> T? {
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
    
    func release(_ connection: T) {
        if let index = connections.firstIndex(where: { $0.value as AnyObject === connection as AnyObject }) {
            connections[index].inUse = false
        }
    }
}