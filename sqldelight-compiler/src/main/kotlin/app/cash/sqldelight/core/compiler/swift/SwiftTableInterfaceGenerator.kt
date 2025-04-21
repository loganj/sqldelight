package app.cash.sqldelight.core.compiler.swift

import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import app.cash.sqldelight.core.lang.psi.ColumnTypeMixin
import app.cash.sqldelight.core.lang.util.columnDefSource
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import io.outfoxx.swiftpoet.PropertySpec
import io.outfoxx.swiftpoet.TypeSpec

internal class SwiftTableInterfaceGenerator(private val table: LazyQuery) {
  private val typeName = allocateName(table.tableName).capitalize()

  fun swiftImplementationSpec(): TypeSpec {
    val structSpec = TypeSpec.structBuilder(typeName)

    // TODO: documentation
    // TODO: adapters
    // TODO: type mixins

    table.query.columns.forEach { queryColumn ->
      val column = queryColumn.element as NamedElement
      val columnName = allocateName(column)
      val columnDef = column.columnDefSource()!!
      val columnType = columnDef.columnType as ColumnTypeMixin
      val swiftType = queryColumn.nullable?.let { isNullable ->
        if (isNullable) columnType.type().asNullable().dialectType.toSwiftType().typeName
        else columnType.type().asNonNullable().dialectType.toSwiftType().typeName
      } ?: columnType.type().dialectType.toSwiftType().typeName

      val propertySpec = PropertySpec.builder(columnName, swiftType)
        .mutable(true)
        .build()
      structSpec.addProperty(propertySpec)
    }

    return structSpec.build()
  }
}
