import Foundation

/// Protocol for database statements
public protocol SQLDelightStatement {
    /// The SQL text of this statement
    var sql: String { get }
    
    /// Execute the statement
    func execute() throws
    
    /// Bind parameters to the statement
    func bind(_ parameters: [Any?]) throws
    
    /// Step to the next row
    func step() throws -> Bool
    
    /// Get a column value
    func column(at index: Int32) throws -> Any?
    
    /// Reset the statement for reuse
    func reset() throws
    
    /// Clear all bindings
    func clearBindings() throws
}