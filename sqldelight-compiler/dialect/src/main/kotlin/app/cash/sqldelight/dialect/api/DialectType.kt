package app.cash.sqldelight.dialect.api

interface DialectType {
  fun toKotlinType(): KotlinType
  fun toSwiftType(): SwiftType = error("no associated Swift type")
}
