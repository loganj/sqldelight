package app.cash.sqldelight.dialect.api

/**
 * Types which are retrieved the same way for all dialects.
 */
enum class PrimitiveType(private val kotlinType: KotlinPrimitive, private val swiftType: SwiftPrimitive) : DialectType {
  ARGUMENT(KotlinPrimitive.Argument, SwiftPrimitive.Argument),
  NULL(KotlinPrimitive.KotlinNull, SwiftPrimitive.SwiftNull),
  INTEGER(KotlinPrimitive.Integer, SwiftPrimitive.Integer),
  REAL(KotlinPrimitive.Real, SwiftPrimitive.Real),
  TEXT(KotlinPrimitive.Text, SwiftPrimitive.Text),
  BOOLEAN(KotlinPrimitive.Boolean, SwiftPrimitive.Boolean),
  BLOB(KotlinPrimitive.Blob, SwiftPrimitive.Blob),
  ;

  override fun toKotlinType(): KotlinType = kotlinType

  override fun toSwiftType(): SwiftType = swiftType;
}
