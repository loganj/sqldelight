package app.cash.sqldelight.core.compiler

import app.cash.sqldelight.core.SqlDelightDatabaseName
import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.compiler.model.NamedQuery
import app.cash.sqldelight.core.compiler.swift.SwiftTableInterfaceGenerator
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.core.lang.util.sqFile
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import io.outfoxx.swiftpoet.FileSpec
import io.outfoxx.swiftpoet.NameAllocator
import java.io.Closeable

object SwiftBackend : Backend() {
  override fun writeTableInterface(
    packageName: String,
    query: LazyQuery,
    statement: PsiElement,
    output: (fileName: String) -> Appendable,
  ) {
    val fileSpec = FileSpec.builder(packageName, allocateName(query.tableName))
      .apply {
        tryWithElement(statement) {
          val generator = SwiftTableInterfaceGenerator(query)
          addType(generator.swiftImplementationSpec())
        }
      }
      .build()

    statement.sqFile().generatedDirectories?.forEach { directory ->
      fileSpec.writeToAndClose(output("$directory/${allocateName(query.tableName).capitalize()}.swift"))
    }
  }

  override fun writeDatabaseInterface(
    module: Module,
    file: SqlDelightFile,
    packageName: String,
    implementationFolder: String,
    dependencies: List<SqlDelightDatabaseName>,
    outputPaths: List<String>,
    output: (fileName: String) -> Appendable,
  ) {
  }

  override fun needsInterface(query: NamedQuery): Boolean {
    return false
  }

  override fun needsWrapper(query: NamedQuery): Boolean {
    return false
  }

  override fun writeQueryInterface(
    namedQuery: NamedQuery,
    file: SqlDelightFile,
    output: (fileName: String) -> Appendable,
  ) {
  }

  override fun writeQueries(
    module: Module,
    file: SqlDelightQueriesFile,
    dialect: SqlDelightDialect,
    packageName: String,
    output: (fileName: String) -> Appendable,
  ) {
  }

  override fun writeImplementations(
    module: Module,
    sourceFile: SqlDelightFile,
    packageName: String,
    className: String,
    implementationFolder: String,
    outputDirectories: List<String>,
    output: (fileName: String) -> Appendable,
  ) {
  }

  override fun allocateName(namedElement: NamedElement): String {
    return NameAllocator().newName(namedElement.normalizedName)
  }
}

private fun FileSpec.writeToAndClose(appendable: Appendable) {
  writeTo(appendable)
  if (appendable is Closeable) appendable.close()
}
