import Foundation

/// Errors that can be thrown by SQLDelight
public enum SQLDelightError: Error {
    /// No results were returned when at least one was expected
    case noResults(String)
    
    /// Multiple results were returned when only one was expected
    case multipleResults(String)
    
    /// A type mismatch occurred when converting database values
    case typeMismatch(expected: Any.Type, actual: Any.Type)
    
    /// A composite error containing both an original error and a rollback error
    case compositeError(message: String, original: Error, rollback: Error)
    
    /// An optimistic lock violation occurred
    case optimisticLock(String)
    
    /// A general database error occurred
    case databaseError(String)
}