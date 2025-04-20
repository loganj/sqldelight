package app.cash.sqldelight.core.compiler.swift

import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import app.cash.sqldelight.core.lang.psi.ColumnTypeMixin
import app.cash.sqldelight.core.lang.util.columnDefSource
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import io.outfoxx.swiftpoet.PropertySpec
import io.outfoxx.swiftpoet.TypeSpec

internal class TableProtocolGenerator(private val table: LazyQuery) {
  private val typeName = allocateName(table.tableName).capitalize()

  fun swiftImplementationSpec(): TypeSpec {
    val typeSpec = TypeSpec.structBuilder(typeName)

    // TODO: documentation
    // TODO: adapters
    // TODO: type mixins

    table.query.columns.forEach { queryColumn ->
      val column = queryColumn.element as NamedElement
      val columnName = allocateName(column)
      val columnDef = column.columnDefSource()!!
      val columnType = columnDef.columnType as ColumnTypeMixin

//      val property = PropertySpec.builder(columnName, columnType.type().dialectType.toSwiftType()).build()
//      typeSpec.addProperty(property)
    }

    return typeSpec.build()
  }

}
