import XCTest
import SQLite
@testable import SQLDelight

final class SQLDelightTests: XCTestCase {
    var db: Connection!
    var driver: TestDatabaseDriver!
    
    override func setUp() async throws {
        db = try Connection(.inMemory)
        driver = TestDatabaseDriver(db: db)
        
        // Create test table
        try db.execute("""
            CREATE TABLE test (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                age INTEGER
            )
        """)
    }
    
    func testSimpleQuery() async throws {
        // Insert test data
        try db.run("INSERT INTO test (id, name, age) VALUES (?, ?, ?)", 1, "Alice", 30)
        
        // Test query execution
        let result = try await driver.execute("SELECT * FROM test WHERE id = 1", mapper: { stmt in
            TestRow(
                id: try stmt.column(at: 0) as! Int64,
                name: try stmt.column(at: 1) as! String,
                age: try stmt.column(at: 2) as! Int64
            )
        })
        
        XCTAssertEqual(result.id, 1)
        XCTAssertEqual(result.name, "Alice")
        XCTAssertEqual(result.age, 30)
    }
    
    func testTransaction() async throws {
        let result = try await driver.transaction { tx in
            try await tx.connection.execute("INSERT INTO test (id, name, age) VALUES (1, 'Bob', 25)")
            return true
        }
        
        XCTAssertTrue(result)
        
        let row = try await driver.execute("SELECT * FROM test WHERE id = 1", mapper: { stmt in
            TestRow(
                id: try stmt.column(at: 0) as! Int64,
                name: try stmt.column(at: 1) as! String,
                age: try stmt.column(at: 2) as! Int64
            )
        })
        
        XCTAssertEqual(row.name, "Bob")
    }
    
    func testColumnAdapter() async throws {
        // Create table with enum column
        try db.execute("""
            CREATE TABLE users (
                id INTEGER PRIMARY KEY,
                status TEXT NOT NULL
            )
        """)
        
        // Insert test data
        try db.run("INSERT INTO users (id, status) VALUES (?, ?)", 1, "active")
        
        // Test query with enum adapter
        let result = try await driver.execute("SELECT * FROM users WHERE id = 1", mapper: { stmt in
            UserRow(
                id: try stmt.column(at: 0) as! Int64,
                status: UserStatus(rawValue: try stmt.column(at: 1) as! String)!
            )
        })
        
        XCTAssertEqual(result.status, .active)
    }
}

// Test support types
struct TestRow {
    let id: Int64
    let name: String
    let age: Int64
}

enum UserStatus: String {
    case active
    case inactive
}

struct UserRow {
    let id: Int64
    let status: UserStatus
}

/// Test implementation of DatabaseDriver using SQLite.swift
actor TestDatabaseDriver {
    private let db: Connection
    private var activeTransaction: TransactionImpl?
    
    init(db: Connection) {
        self.db = db
    }
}

extension TestDatabaseDriver: SqlDriver {
    var currentTransaction: Transaction? {
        get async { activeTransaction }
    }
    
    func execute<T>(_ sql: String, mapper: @escaping (SQLDelightStatement) throws -> T) async throws -> T {
        let sqliteStmt = try db.prepare(sql)
        let stmt = SQLiteStatementAdapter(sqliteStmt)
        guard try stmt.step() else {
            throw SQLDelightError.noResults("Query returned no results")
        }
        return try mapper(stmt)
    }
    
    func execute(_ sql: String) async throws {
        try db.execute(sql)
    }
    
    func transaction<T>(_ block: @escaping (Transaction) async throws -> T) async throws -> T {
        guard activeTransaction == nil else {
            throw SQLiteError.transactionActive
        }
        
        // Start transaction
        try await execute("BEGIN TRANSACTION")
        
        let transaction = TransactionImpl(connection: SQLiteConnection(db: self.db))
        activeTransaction = transaction
        defer { activeTransaction = nil }
        
        do {
            let result = try await block(transaction)
            try await execute("COMMIT")
            transaction.commit()
            return result
        } catch {
            try? await execute("ROLLBACK")
            transaction.rollback()
            throw error
        }
    }
    
    func addListener(queryKeys: [String], listener: QueryListener) async {
        // Not implemented in test driver
    }
    
    func removeListener(queryKeys: [String], listener: QueryListener) async {
        // Not implemented in test driver
    }
    
    func notifyListeners(queryKeys: [String]) async {
        // Not implemented in test driver
    }
}

/// SQLite.swift connection adapter
class SQLiteConnection: DatabaseConnection {
    private let db: Connection
    
    init(db: Connection) {
        self.db = db
    }
    
    func prepare(_ sql: String) async throws -> SQLDelightStatement {
        SQLiteStatementAdapter(try db.prepare(sql))
    }
    
    func execute(_ sql: String) async throws {
        try db.execute(sql)
    }
    
    func beginTransaction() async throws {
        try db.execute("BEGIN TRANSACTION")
    }
    
    func commit() async throws {
        try db.execute("COMMIT")
    }
    
    func rollback() async throws {
        try db.execute("ROLLBACK")
    }
    
    var isInTransaction: Bool {
        // SQLite.swift doesn't expose this directly
        // In a real implementation we'd track this ourselves
        false
    }
}