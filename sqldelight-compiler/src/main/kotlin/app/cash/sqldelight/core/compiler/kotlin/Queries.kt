package app.cash.sqldelight.core.compiler.kotlin

import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.compiler.SqlDelightCompiler
import app.cash.sqldelight.core.compiler.model.BindableQuery
import app.cash.sqldelight.core.compiler.model.NamedQuery
import app.cash.sqldelight.core.decapitalize
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.core.lang.acceptsTableInterface
import app.cash.sqldelight.core.lang.parentAdapter
import app.cash.sqldelight.core.lang.util.sqFile
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.SelectQueryable
import com.alecstrong.sql.psi.core.psi.SqlCompoundSelectStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateVirtualTableStmt
import com.alecstrong.sql.psi.core.psi.SqlInsertStmt
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.PropertySpec

/**
 * The collection of parameters exposed in the generated api for this query.
 */
fun BindableQuery.getParameters(): List<IntermediateType> {
  if (statement is SqlInsertStmt && statement.acceptsTableInterface()) {
    val table = statement.tableName.reference!!.resolve()!!
    return listOf(
      IntermediateType(
        PrimitiveType.ARGUMENT,
        javaType = ClassName(
          table.sqFile().packageName!!,
          SqlDelightCompiler.allocateName(statement.tableName).capitalize()
        ),
        name = SqlDelightCompiler.allocateName(statement.tableName),
      ),
    )
  }
  return arguments.sortedBy { it.index }.map { it.type }
}

/**
 * Explodes the sqlite query into an ordered list (same order as the query) of adapters required for
 * the types to be exposed by the generated api.
 */
internal fun NamedQuery.getResultColumnRequiredAdapters(): List<PropertySpec> {
  return if (queryable is SelectQueryable) {
    resultColumnRequiredAdapters(queryable.select)
  } else {
    queryable.select.typesExposed(LinkedHashSet()).mapNotNull { it.parentAdapter() }
  }
}

private fun NamedQuery.resultColumnRequiredAdapters(select: SqlCompoundSelectStmt): List<PropertySpec> {
  val namesUsed = LinkedHashSet<String>()

  return select.selectStmtList.flatMap { select ->
    if (select.valuesExpressionList.isNotEmpty()) {
      resultColumns(select.valuesExpressionList)
    } else {
      select.typesExposed(namesUsed)
    }.mapNotNull { it.parentAdapter() }
  }
}

/**
 * The name of the generated interface that this query references. The linked interface will have
 * a default implementation subclass.
 */
internal fun NamedQuery.getInterfaceType(): ClassName {

  val pureTable = pureTable
  if (pureTable != null && pureTable.parent !is SqlCreateVirtualTableStmt) {
    return ClassName(pureTable.sqFile().packageName!!, SqlDelightCompiler.allocateName(pureTable).capitalize())
  }
  var packageName = queryable.select.sqFile().packageName!!
  if (queryable.select.sqFile().parent?.files
      ?.filterIsInstance<SqlDelightQueriesFile>()?.flatMap { it.namedQueries }
      ?.filter { it.needsInterface() && it != this }
      ?.any { it.name == name } == true
  ) {
    packageName = "$packageName.${queryable.select.sqFile().virtualFile!!.nameWithoutExtension.decapitalize()}"
  }
  return ClassName(packageName, name.capitalize())
}
