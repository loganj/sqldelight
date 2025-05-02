# SQLDelight Swift Runtime

A native Swift runtime implementation for SQLDelight that provides database access, connection management, and query execution.

## Overview

This library provides the core runtime functionality needed by SQLDelight-generated Swift code. It implements the database access patterns, connection management, transaction handling, and type adaptation required to work with SQLite databases in Swift applications.

## Key Features

- Async/await-based database operations
- Actor-based thread safety
- Connection pooling with read/write separation
- Transaction management
- Type-safe query execution
- Custom column adapters
- Query change listeners

## Architecture

The runtime is built around several core abstractions:

- **SqlDriver**: Main entry point for database operations
- **DatabaseConnection**: Manages database connections and statement preparation
- **Transaction**: Handles transaction lifecycle and commit/rollback
- **SQLDelightStatement**: Wraps database statements and result access
- **Query**: Type-safe query execution and result mapping
- **ColumnAdapter**: Custom type conversion

## Comparison with Kotlin Runtime

While this implementation provides the same core functionality as the Kotlin runtime, it takes advantage of Swift's unique language features and follows Swift idioms.

### Key Similarities

- Core abstractions and interfaces
- Connection pooling strategy
- Transaction management patterns
- Type safety and adaptation
- Query result handling
- Change notification system

### Key Differences

#### Thread Safety
- **Kotlin**: Manual thread confinement checks
- **Swift**: Actor-based isolation

#### Asynchronous Operations
- **Kotlin**: Coroutines and suspending functions
- **Swift**: Native async/await with structured concurrency

#### Error Handling
- **Kotlin**: Result type and exceptions
- **Swift**: Native throws/try system

#### API Design
- **Kotlin**: Extension functions and receivers
- **Swift**: Protocol-oriented design with closures

#### Type System
- **Kotlin**: Nullable types with platform types
- **Swift**: Optional types with strict nullability

## Requirements

- Swift 5.5+
- iOS 13.0+

## Integration

This runtime is designed to work with SQLDelight's Swift code generator. It provides the runtime support needed for the generated database access code.

## Thread Safety

The runtime uses Swift's actor system to provide thread-safe database access. All database operations are automatically serialized through the appropriate actor, eliminating the need for manual synchronization.

## Transactions

Transactions are fully asynchronous and support:
- Nested transactions
- Commit/rollback handling
- Post-commit/rollback hooks
- Error handling with automatic rollback

## Query Execution

Queries are type-safe and support:
- Custom result mapping
- Column type adaptation
- Change notification
- Asynchronous execution
- Error handling

## Type Adaptation

The runtime includes a flexible type adaptation system that supports:
- Custom type mapping
- Enum handling
- Optional values
- Collection types
