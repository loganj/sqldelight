import Foundation

/// Protocol for transaction lifecycle callbacks
public protocol TransactionCallbacks {
    /// Register a closure to run after commit
    func afterCommit(_ function: @escaping () -> Void)
    
    /// Register a closure to run after rollback
    func afterRollback(_ function: @escaping () -> Void)
}

/// A database transaction
public protocol Transaction: TransactionCallbacks {
    /// Get the transaction's connection
    var connection: any DatabaseConnection { get }
}

/// Default transaction implementation
public class TransactionImpl: Transaction {
    public let connection: any DatabaseConnection
    private var postCommitHooks: [() -> Void] = []
    private var postRollbackHooks: [() -> Void] = []
    
    public init(connection: any DatabaseConnection) {
        self.connection = connection
    }
    
    public func afterCommit(_ function: @escaping () -> Void) {
        postCommitHooks.append(function)
    }
    
    public func afterRollback(_ function: @escaping () -> Void) {
        postRollbackHooks.append(function)
    }
    
    func commit() {
        postCommitHooks.forEach { $0() }
        postCommitHooks.removeAll()
    }
    
    func rollback() {
        postRollbackHooks.forEach { $0() }
        postRollbackHooks.removeAll()
    }
}

/// Error thrown when a transaction is rolled back
public struct RollbackError: Error {
    public let returnValue: Any?
    
    public init(returnValue: Any? = nil) {
        self.returnValue = returnValue
    }
}