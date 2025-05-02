import Foundation

/// A result type that can be either synchronous or asynchronous
public enum QueryResult<T> {
    /// A synchronous value
    case value(T)
    
    /// An asynchronous value
    case asyncValue(any AsyncValue<T>)
    
    /// Get the value, waiting if necessary
    public func value() async throws -> T {
        switch self {
        case .value(let value):
            return value
        case .asyncValue(let asyncValue):
            return try await asyncValue.value()
        }
    }
}

/// Protocol for asynchronous values
public protocol AsyncValue<T> {
    associatedtype T
    func value() async throws -> T
}