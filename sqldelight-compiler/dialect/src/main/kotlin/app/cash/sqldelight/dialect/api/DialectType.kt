package app.cash.sqldelight.dialect.api

interface DialectType {
  fun toKotlinType(): KotlinType
}
