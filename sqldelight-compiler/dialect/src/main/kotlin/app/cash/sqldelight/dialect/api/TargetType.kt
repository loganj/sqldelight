package app.cash.sqldelight.dialect.api


typealias KotlinType = TargetType<com.squareup.kotlinpoet.TypeName, com.squareup.kotlinpoet.CodeBlock>
typealias SwiftType = TargetType<io.outfoxx.swiftpoet.TypeName, io.outfoxx.swiftpoet.CodeBlock>

interface TargetType<TypeNameType, CodeBlockType> {
  val typeName: TypeNameType
  fun decode(value: CodeBlockType): CodeBlockType = value
  fun encode(value: CodeBlockType): CodeBlockType = value
  fun prepareStatementBinder(columnIndex: CodeBlockType, value: CodeBlockType): CodeBlockType
  fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlockType
}
