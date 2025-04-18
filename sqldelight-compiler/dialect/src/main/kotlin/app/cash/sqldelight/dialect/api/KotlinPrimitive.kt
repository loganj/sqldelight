package app.cash.sqldelight.dialect.api

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

internal sealed class KotlinPrimitive(
  override val typeName: TypeName,
  val binderName: String?,
  val getterName: String?,
) : KotlinType {

  override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
    return CodeBlock.of("$cursorName.$getterName($columnIndex)")
  }

  override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock): CodeBlock {
    return CodeBlock.builder()
      .add(binderName!!)
      .add("(%L, %L)\n", columnIndex, value)
      .build()
  }

  object Argument : KotlinPrimitive(ANY.copy(nullable = true), null, null) {
    override fun cursorGetter(columnIndex: Int, cursorName: String) =
      throw IllegalArgumentException("Cannot retrieve argument from cursor")

    override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock) =
      throw IllegalArgumentException("Cannot bind unknown types or null")
  }

  object KotlinNull : KotlinPrimitive(Nothing::class.asClassName().copy(nullable = true), null, null) {
    override fun cursorGetter(columnIndex: Int, cursorName: String) = CodeBlock.of("null")

    override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock) =
      throw IllegalArgumentException("Cannot bind unknown types or null")
  }

  object Integer : KotlinPrimitive(LONG, "bindLong", "getLong")
  object Real : KotlinPrimitive(DOUBLE, "bindDouble", "getDouble")
  object Text : KotlinPrimitive(String::class.asTypeName(), "bindString", "getString")
  object Blob : KotlinPrimitive(ByteArray::class.asTypeName(), "bindBytes", "getBytes")
  object Boolean : KotlinPrimitive(com.squareup.kotlinpoet.BOOLEAN, "bindBoolean", "getBoolean")
}
