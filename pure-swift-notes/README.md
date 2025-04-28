# SQLDelight Pure Swift Implementation

This project extends SQLDelight to support Swift code generation and provides a native Swift runtime library.

## Overview

### Compiler
The Swift implementation extends SQLDelight's existing compiler:
- [Core Architecture](compiler.md#core-architecture): Compiler interface and implementation
- [SQL Support](compiler.md#sql-support): SQL dialect and type declarations
- [Generated Code](compiler.md#generated-code): Swift code generation

### Build Integration
Support for various build systems:
- [Command Line Interface](build-integration.md#command-line-interface): CLI tool
- [Gradle Plugin](build-integration.md#gradle-plugin): Development and testing
- [Bazel Integration](build-integration.md#bazel-integration): Build rules

### Runtime Library
Native Swift implementation of SQLDelight's runtime:
- [Core Types](core-types.md): Query<T>, adapters, and interfaces
- [Connection Management](connection.md): Pooling and lifecycle
- [Thread Safety](thread-safety.md): Actor-based protection

## Implementation Strategy

1. **Compiler Extensions**
   - Extract SqlDelightCompiler interface
   - Implement SqlDelightSwiftCompiler
   - Generate idiomatic Swift code
   - Maintain SQLDelight's SQL support

2. **Runtime Development**
   - Core types and interfaces
   - Connection management
   - Thread safety
   - Resource handling

3. **Integration & Testing**
   - CLI tool implementation
   - Build system integration
   - Documentation
   - Testing infrastructure

## Documentation

- [compiler.md](compiler.md): Compiler architecture and code generation
- [build-integration.md](build-integration.md): Build system support
- [core-types.md](core-types.md): Runtime type system
- [connection.md](connection.md): Connection management
- [thread-safety.md](thread-safety.md): Concurrency model