package app.cash.sqldelight.dialect.api

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

typealias KotlinType = TargetType<TypeName, CodeBlock>

interface TargetType<TypeNameType, CodeBlockType> {
  val typeName: TypeNameType
  fun decode(value: CodeBlockType): CodeBlockType = value
  fun encode(value: CodeBlockType): CodeBlockType = value
  fun prepareStatementBinder(columnIndex: CodeBlockType, value: CodeBlockType): CodeBlockType
  fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlockType
}
