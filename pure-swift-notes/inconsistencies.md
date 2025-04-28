# SQLDelight Swift Implementation Inconsistencies

@goose IMPORTANT INSTRUCTION: when making updates in response to @goose notes, be careful not to truncate or lose content.  when you've addressed a @goose note in this file, remove it unless otherwise instructed in the note.

## 1. Async/Await Usage
- `concurrency.md` shows synchronous Query<T> return types
- `code-generation.md` also shows synchronous Query<T>
- `memory-management.md` has some async functions
- Need to make consistent whether we're using async/await or not

Note: Checking against Kotlin implementation:
- Query interface is synchronous (returns Query<T>)
- Database operations are synchronous
- No async/await usage
Therefore `concurrency.md` and `code-generation.md` are correct with synchronous Query<T>, while `memory-management.md` needs updating.

@goose are the existing kotlin driver or transaction async?

## 2. Compiler Architecture
- `build.md` shows extracting SqlDelightCompiler interface
- `sql-parsing.md` still refers to extending SQLDelight's parser directly
- `code-generation.md` references a SwiftDialect that shouldn't exist
- Need to align all files with the compiler interface approach

@goose fix the SwiftDialect reference and make the other files consistent with the approach in the build notes

## 3. Type System
- `type-system.md` shows Int for INTEGER
- Some examples in other files still use Int64
- Need to consistently use Int throughout

@goose consistently use Int throughout

## 4. Error Handling
- `build.md` has detailed error handling comparison
- Other files have different or no error handling examples
- Should standardize error handling approach across files

@goose leave this alone, you can remove this from our list.

## 5. Memory Management
- `memory-management.md` discusses connection pools
- `concurrency.md` also discusses connection management
- Need to consolidate connection management in one place

@goose leave this one for now, and also leave this @goose note in. we'll come back to it.

## 6. Build System Integration
- `build.md` shows both Kotlin and Swift configuration
- Other files might reference old configuration style
- Need to update all build system references

@goose don't update any files for this. check whether other files reference the old style, and leave a note here telling me whether they do.

Note: No other files reference build configuration styles. Only `build.md` contains build configuration examples.

## 7. Generated Code Examples
- Different files show slightly different versions of generated code
- Need to make generated code examples consistent

@goose leave a note here showing examples of inconsistency. be sure to note which file the examples come from.

Examples of inconsistency:

`code-generation.md`:
```swift
struct User {
    let id: Int
    let name: String?
    let createdAt: Date
}
```

`type-system.md`:
```swift
struct User {
    let id: Int
    let name: String?
    let tags: [String]
}
```

`memory-management.md`:
```swift
struct User {
    let id: Int64  // Uses Int64 instead of Int
    let name: String?
    let createdAt: Date
}
```

## 8. Wire References
- Some files still have Wire-inspired sections
- Need to remove these and incorporate accepted recommendations

@goose don't update any other files for this.  do update this file to indicate which other files still have wire references, specifically.

Files with Wire references:
- `code-generation.md`: Has "Wire-Inspired Recommendations" section
- `memory-management.md`: Has "Wire-Inspired Recommendations" section
- `sql-parsing.md`: Has "Wire-Inspired Recommendations" section
- `concurrency.md`: Has "Wire-Inspired Recommendations" section