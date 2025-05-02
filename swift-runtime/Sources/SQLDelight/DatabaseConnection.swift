import Foundation

/// Protocol for database connections
public protocol DatabaseConnection: AnyObject {
    /// Prepare a SQL statement
    func prepare(_ sql: String) async throws -> SQLDelightStatement
    
    /// Execute a SQL statement
    func execute(_ sql: String) async throws
    
    /// Begin a transaction
    func beginTransaction() async throws
    
    /// Commit the current transaction
    func commit() async throws
    
    /// Rollback the current transaction
    func rollback() async throws
    
    /// Check if a transaction is active
    var isInTransaction: Bool { get }
}