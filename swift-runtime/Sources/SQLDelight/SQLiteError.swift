import Foundation

/// Standard SQLite errors
public enum SQLiteError: Error {
    case connectionClosed
    case transactionActive
    case prepareFailed(code: Int32)
    case bindFailed(code: Int32)
    case executionFailed(code: Int32)
}