import Foundation

/// A cursor over a result set from a SQL query
public protocol SqlCursor {
    /// Advance to the next row
    func next() throws -> QueryResult<Bool>
    
    /// Get a String value from the current row
    func getString(index: Int32) throws -> String?
    
    /// Get a Long value from the current row
    func getLong(index: Int32) throws -> Int64?
    
    /// Get a Bytes value from the current row
    func getBytes(index: Int32) throws -> Data?
    
    /// Get a Double value from the current row
    func getDouble(index: Int32) throws -> Double?
    
    /// Get a Boolean value from the current row
    func getBoolean(index: Int32) throws -> Bool?
}

/// A prepared SQL statement that can have values bound to its parameters
public protocol SqlPreparedStatement {
    /// Bind a String value to a parameter
    func bindString(index: Int32, value: String?) throws
    
    /// Bind a Long value to a parameter
    func bindLong(index: Int32, value: Int64?) throws
    
    /// Bind a Bytes value to a parameter
    func bindBytes(index: Int32, value: Data?) throws
    
    /// Bind a Double value to a parameter
    func bindDouble(index: Int32, value: Double?) throws
    
    /// Bind a Boolean value to a parameter
    func bindBoolean(index: Int32, value: Bool?) throws
}