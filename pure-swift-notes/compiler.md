# SQLDelight Swift Compiler

## Core Architecture

SQLDelight's Swift implementation extends the existing compiler infrastructure:

```kotlin
interface SqlDelightCompiler {
    fun writeTableInterfaces(
        file: SqlDelightFile,
        output: FileAppender
    )
    
    fun writeQueryInterfaces(
        file: SqlDelightQueriesFile,
        output: FileAppender
    )
    
    fun writeImplementations(
        module: Module,
        sourceFile: SqlDelightFile,
        implementationFolder: String,
        output: FileAppender
    )
}

// Existing Kotlin compiler renamed
class SqlDelightKotlinCompiler : SqlDelightCompiler {
    // Existing implementation
}

// New Swift compiler
class SqlDelightSwiftCompiler : SqlDelightCompiler {
    override fun writeTableInterfaces(
        file: SqlDelightFile,
        output: FileAppender
    ) {
        // Generate Swift table interfaces
    }
    
    override fun writeQueryInterfaces(
        file: SqlDelightQueriesFile,
        output: FileAppender
    ) {
        // Generate Swift query interfaces
    }
    
    override fun writeImplementations(
        module: Module,
        sourceFile: SqlDelightFile,
        implementationFolder: String,
        output: FileAppender
    ) {
        // Generate Swift implementations
    }
}
```

## SQL Support

SQLDelight's Swift implementation uses the existing SQL parser and type system:

1. **SQLite Dialect Compatibility**
```sql
CREATE TABLE user (
  id INTEGER PRIMARY KEY,
  name TEXT,
  created_at INTEGER
);

CREATE INDEX user_name ON user(name);
```

2. **Swift Type Declarations**
```sql
CREATE TABLE user (
  -- Basic Swift types
  id INTEGER AS Int NOT NULL,
  name TEXT AS String?,
  active INTEGER AS Bool,
  data BLOB AS Data,
  
  -- Custom types
  preferences TEXT AS UserPreferences,
  theme TEXT AS Theme.Style  -- Nested type
);
```

3. **Type Resolution**
```kotlin
class SqlDelightSwiftCompiler : SqlDelightCompiler {
    override fun writeTableInterfaces(
        file: SqlDelightFile,
        output: FileAppender
    ) {
        file.tables().forEach { table ->
            // Map SQLite types to Swift types
            val columns = table.columns.map { column ->
                val swiftType = when(column.type) {
                    is SqliteType.INTEGER -> if (column.nullable) "Int?" else "Int"
                    is SqliteType.REAL -> if (column.nullable) "Double?" else "Double"
                    is SqliteType.TEXT -> if (column.nullable) "String?" else "String"
                    is SqliteType.BLOB -> if (column.nullable) "Data?" else "Data"
                }
                Column(column.name, swiftType)
            }
            
            // Generate Swift struct
            output.append(generateTableStruct(table.name, columns))
        }
    }
}
```

## Generated Code

1. **Table Interfaces**
```swift
struct User {
    let id: Int
    let name: String?
    let createdAt: Date
}
```

2. **Query Interfaces**
```swift
extension Database {
    /// - Parameter identifier: User ID to find
    /// - Returns: Query that will return User if found, nil if not found
    /// - Throws: SQLError on database errors
    func user(with identifier: Int) -> Query<User?>
    
    /// - Parameter name: Name to match
    /// - Returns: Query that will return array of matching users
    /// - Throws: SQLError on database errors
    func users(matching name: String) -> Query<[User]>
}
```

3. **Database Implementation**
```swift
private struct UserQuery: Query<User?> {
    private let sql = """
        SELECT id, name, created_at
        FROM user
        WHERE id = ?
        """
    
    private let identifier: Int
    private let dateAdapter: DateAdapter
    
    func execute(driver: DatabaseDriver) throws -> User? {
        let statement = try driver.prepare(sql)
        defer { statement.finalize() }
        
        try statement.bind(identifier, at: 1)
        
        guard try statement.step() else {
            return nil
        }
        
        return User(
            id: try statement.integer(at: 0),
            name: try statement.stringOrNull(at: 1),
            createdAt: dateAdapter.decode(try statement.integer(at: 2))
        )
    }
    
    override var description: String { "User.sq:user" }
}
```

4. **Generated Types**
```swift
// Type adapters used internally by query implementations
struct DateAdapter: ColumnAdapter {
    func decode(_ databaseValue: Int) -> Date {
        Date(timeIntervalSince1970: TimeInterval(databaseValue))
    }
    
    func encode(_ value: Date) -> Int {
        Int(value.timeIntervalSince1970)
    }
}
```

5. **Documentation Generation**
```swift
/// - Parameter identifier: User ID to find
/// - Returns: User if found, nil if not found
/// - Throws: SQLError on database errors
func user(with identifier: Int) -> Query<User?>

// Source reference in description
override var description: String { "User.sq:user" }
```