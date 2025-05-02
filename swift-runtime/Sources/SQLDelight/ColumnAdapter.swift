import Foundation

/// Protocol for converting between database and model types
public protocol ColumnAdapter {
    associatedtype ModelType
    associatedtype DatabaseType
    
    func decode(_ databaseValue: DatabaseType) -> ModelType
    func encode(_ value: ModelType) -> DatabaseType
}

/// Default adapter for enum types
public struct EnumColumnAdapter<T: RawRepresentable>: ColumnAdapter where T.RawValue: DatabaseValue {
    public typealias ModelType = T
    public typealias DatabaseType = T.RawValue
    
    public init() {}
    
    public func decode(_ databaseValue: DatabaseType) -> ModelType {
        guard let value = ModelType(rawValue: databaseValue) else {
            fatalError("Invalid enum value: \(databaseValue)")
        }
        return value
    }
    
    public func encode(_ value: ModelType) -> DatabaseType {
        value.rawValue
    }
}

/// Protocol for types that can be stored in the database
public protocol DatabaseValue {
    func asDatabaseValue() -> Any
    static func fromDatabaseValue(_ value: Any) throws -> Self
}

// Default implementations for standard types
extension String: DatabaseValue {
    public func asDatabaseValue() -> Any { self }
    public static func fromDatabaseValue(_ value: Any) throws -> String {
        guard let string = value as? String else {
            throw SQLiteError.executionFailed(code: -1)
        }
        return string
    }
}

extension Int64: DatabaseValue {
    public func asDatabaseValue() -> Any { self }
    public static func fromDatabaseValue(_ value: Any) throws -> Int64 {
        guard let int = value as? Int64 else {
            throw SQLiteError.executionFailed(code: -1)
        }
        return int
    }
}

extension Double: DatabaseValue {
    public func asDatabaseValue() -> Any { self }
    public static func fromDatabaseValue(_ value: Any) throws -> Double {
        guard let double = value as? Double else {
            throw SQLiteError.executionFailed(code: -1)
        }
        return double
    }
}

extension Data: DatabaseValue {
    public func asDatabaseValue() -> Any { self }
    public static func fromDatabaseValue(_ value: Any) throws -> Data {
        guard let data = value as? Data else {
            throw SQLiteError.executionFailed(code: -1)
        }
        return data
    }
}