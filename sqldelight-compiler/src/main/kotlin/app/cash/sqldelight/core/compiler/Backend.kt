package app.cash.sqldelight.core.compiler

import app.cash.sqldelight.core.SqlDelightDatabaseName
import app.cash.sqldelight.core.compiler.model.NamedQuery
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement

abstract sealed class Backend {
  abstract fun writeTableInterface(
    packageName: String,
    query: LazyQuery,
    statement: PsiElement,
    output: (fileName: String) -> Appendable,
  )

  abstract fun writeDatabaseInterface(
    module: Module,
    file: SqlDelightFile,
    packageName: String,
    implementationFolder: String,
    dependencies: List<SqlDelightDatabaseName>,
    outputPaths: List<String>,
    output: (fileName: String) -> Appendable,
  )

  /**
   * @return true if this query needs its own interface generated.
   */
  abstract fun needsInterface(query: NamedQuery): Boolean
  abstract fun needsWrapper(query: NamedQuery): Boolean
  abstract fun writeQueryInterface(
    namedQuery: NamedQuery,
    file: SqlDelightFile,
    output: (fileName: String) -> Appendable,
  )

  abstract fun writeQueries(
    module: Module,
    file: SqlDelightQueriesFile,
    dialect: SqlDelightDialect,
    packageName: String,
    output: (fileName: String) -> Appendable,
  )

  abstract fun writeImplementations(
    module: Module,
    sourceFile: SqlDelightFile,
    packageName: String,
    className: String,
    implementationFolder: String,
    outputDirectories: List<String>,
    output: (fileName: String) -> Appendable,
  )

  abstract fun allocateName(namedElement: NamedElement): String
}

internal val NamedElement.normalizedName: String
  get() {
    val f = name[0]
    val l = name[name.lastIndex]
    return if ((f in "\"'`" && f == l) || (f == '[' && l == ']')) {
      name.substring(1, name.length - 1)
    } else {
      name
    }
  }

