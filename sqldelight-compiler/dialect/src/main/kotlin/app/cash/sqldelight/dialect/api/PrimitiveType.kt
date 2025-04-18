package app.cash.sqldelight.dialect.api

/**
 * Types which are retrieved the same way for all dialects.
 */
enum class PrimitiveType(private val kotlinType: KotlinPrimitive) : DialectType {
  ARGUMENT(KotlinPrimitive.Argument),
  NULL(KotlinPrimitive.KotlinNull),
  INTEGER(KotlinPrimitive.Integer),
  REAL(KotlinPrimitive.Real),
  TEXT(KotlinPrimitive.Text),
  BOOLEAN(KotlinPrimitive.Boolean),
  BLOB(KotlinPrimitive.Blob),
  ;

  override fun toKotlinType(): KotlinType = kotlinType
}
