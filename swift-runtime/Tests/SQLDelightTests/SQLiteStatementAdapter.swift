import Foundation
import SQLite
@testable import SQLDelight

/// Adapter to wrap SQLite.swift's Statement in our SQLDelightStatement protocol
class SQLiteStatementAdapter: SQLDelightStatement {
    private let stmt: SQLite.Statement
    
    var sql: String {
        stmt.description
    }
    
    init(_ stmt: SQLite.Statement) {
        self.stmt = stmt
    }
    
    func execute() throws {
        try stmt.run()
    }
    
    func bind(_ parameters: [Any?]) throws {
        // SQLite.swift handles binding through its run() method
        // This is a no-op for our test implementation
    }
    
    func step() throws -> Bool {
        try stmt.step()
    }
    
    func column(at index: Int32) throws -> Any? {
        // SQLite.swift's Statement.row method returns [Binding?]
        let row = stmt.row
        return row[Int(index)]
    }
    
    func reset() throws {
        stmt.reset()
    }
    
    func clearBindings() throws {
        stmt.reset() // SQLite.swift combines reset and clear bindings
    }
}