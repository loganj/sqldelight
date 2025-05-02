import Foundation

/// Protocol for database drivers
public protocol SqlDriver: AnyObject {
    /// Execute a query with a mapper function
    func execute<T>(_ sql: String, mapper: @escaping (SQLDelightStatement) throws -> T) async throws -> T
    
    /// Execute a SQL statement
    func execute(_ sql: String) async throws
    
    /// Start a new transaction
    func transaction<T>(_ block: @escaping (Transaction) async throws -> T) async throws -> T
    
    /// Get the current transaction if any
    var currentTransaction: Transaction? { get async }
    
    /// Add a query listener
    func addListener(queryKeys: [String], listener: QueryListener) async
    
    /// Remove a query listener
    func removeListener(queryKeys: [String], listener: QueryListener) async
    
    /// Notify query listeners
    func notifyListeners(queryKeys: [String]) async
}