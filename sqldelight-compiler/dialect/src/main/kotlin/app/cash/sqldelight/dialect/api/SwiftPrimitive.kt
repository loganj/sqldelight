package app.cash.sqldelight.dialect.api

import io.outfoxx.swiftpoet.ANY
import io.outfoxx.swiftpoet.BOOL
import io.outfoxx.swiftpoet.CodeBlock
import io.outfoxx.swiftpoet.DATA
import io.outfoxx.swiftpoet.DOUBLE
import io.outfoxx.swiftpoet.DeclaredTypeName
import io.outfoxx.swiftpoet.INT
import io.outfoxx.swiftpoet.OPTIONAL
import io.outfoxx.swiftpoet.STRING
import io.outfoxx.swiftpoet.TypeName
import io.outfoxx.swiftpoet.parameterizedBy

internal sealed class SwiftPrimitive(
  override val typeName: TypeName,
  val binderName: String?,
  val getterName: String?,
) : SwiftType {

  override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock): CodeBlock {
    return CodeBlock.builder()
      .add(binderName!!)
      .add("(%L, %L)\n", columnIndex, value)
      .build()
  }

  override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
    return CodeBlock.of("$cursorName.$getterName($columnIndex)")
  }

  object Argument : SwiftPrimitive(OPTIONAL.parameterizedBy(ANY), null, null) {
    override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
      throw IllegalArgumentException("Cannot retrieve argument from cursor")
    }

    override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock): CodeBlock {
      throw IllegalArgumentException("Cannot bind unknown types or nil")
    }
  }

  object SwiftNull : SwiftPrimitive(DeclaredTypeName.typeName("Swift.Never"), null, null) {
    override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock = CodeBlock.of("nil")

    override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock): CodeBlock {
      throw IllegalArgumentException("Cannot bind unknown types or nil")
    }
  }

  object Integer : SwiftPrimitive(INT, "bindInt", "getInt")
  object Real : SwiftPrimitive(DOUBLE, "bindDouble", "getDouble")
  object Text : SwiftPrimitive(STRING, "bindString", "getString")
  object Blob : SwiftPrimitive(DATA, "bindData", "getData")
  object Boolean : SwiftPrimitive(BOOL, "bindBool", "getBool")
}
