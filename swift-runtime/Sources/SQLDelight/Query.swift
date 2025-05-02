import Foundation

/// A query that returns values of type T
public struct Query<T> {
    private let impl: () throws -> T?
    
    /// Initialize a new query with an implementation closure
    public init(_ impl: @escaping () throws -> T?) {
        self.impl = impl
    }
    
    /// Execute the query and return the result
    public func execute() throws -> T? {
        try impl()
    }
}

/// Protocol for objects that can listen to query changes
public protocol QueryListener: AnyObject {
    func queryResultsChanged()
}

/// Extension to support mapping query results
extension Query {
    /// Map the query results to a different type
    public func map<U>(_ transform: @escaping (T) -> U) -> Query<U> {
        Query<U> {
            guard let value = try self.execute() else { return nil }
            return transform(value)
        }
    }
    
    /// Execute the query and return all results as a list
    public func asList() throws -> [T] {
        guard let result = try execute() else { return [] }
        return [result]
    }
    
    /// Execute the query and return exactly one result
    public func asOne() throws -> T {
        guard let result = try execute() else {
            throw SQLiteError.executionFailed(code: -1) // No results
        }
        return result
    }
    
    /// Execute the query and return one optional result
    public func asOneOrNull() throws -> T? {
        try execute()
    }
}