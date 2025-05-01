/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.sqldelight.core.compiler

import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.compiler.model.NamedQuery
import app.cash.sqldelight.core.lang.MigrationFile
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.dialect.api.SelectQueryable
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import com.alecstrong.sql.psi.core.psi.InvalidElementDetectedException
import com.alecstrong.sql.psi.core.psi.SqlCreateViewStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateVirtualTableStmt
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement

private typealias FileAppender = (fileName: String) -> Appendable

object SqlDelightCompiler {
  fun writeInterfaces(
    module: Module,
    dialect: SqlDelightDialect,
    file: SqlDelightQueriesFile,
    output: FileAppender,
    backend: Backend = KotlinBackend
  ) {
    try {
      writeTableInterfaces(file, output, backend = backend)
      writeQueryInterfaces(file, output, backend)
      writeQueries(module, dialect, file, output, backend)
    } catch (e: InvalidElementDetectedException) {
      // It's okay if compilation is cut short, we can just quit out.
    }
  }

  fun writeInterfaces(
    file: MigrationFile,
    output: FileAppender,
    includeAll: Boolean = false,
    backend: Backend = KotlinBackend
  ) {
    writeTableInterfaces(file, output, includeAll, backend = backend)
  }

  fun writeDatabaseInterface(
    module: Module,
    file: SqlDelightFile,
    implementationFolder: String,
    output: FileAppender,
    backend: Backend = KotlinBackend
  ) {
    val fileIndex = SqlDelightFileIndex.getInstance(module)
    val packageName = fileIndex.packageName
    val outputPaths = fileIndex.outputDirectory(file)
    val dependencies = fileIndex.dependencies

    backend.writeDatabaseInterface(module, file, packageName, implementationFolder, dependencies, outputPaths, output)
  }

  fun writeImplementations(
    module: Module,
    sourceFile: SqlDelightFile,
    implementationFolder: String,
    output: FileAppender,
    backend: Backend
  ) {
    val fileIndex = SqlDelightFileIndex.getInstance(module)
    val packageName = fileIndex.packageName
    val className = fileIndex.className
    val outputDirectories = fileIndex.outputDirectory(sourceFile)

    backend.writeImplementations(module, sourceFile, packageName, className, implementationFolder, outputDirectories, output)
  }

  internal fun writeTableInterfaces(
    file: SqlDelightFile,
    output: FileAppender,
    includeAll: Boolean = false,
    backend: Backend
  ) {
    val packageName = file.packageName ?: return
    file.tables(includeAll).forEach { query ->
      val statement = query.tableName.parent

      if (statement is SqlCreateViewStmt && statement.compoundSelectStmt != null) {
        listOf(NamedQuery(backend.allocateName(statement.viewName), SelectQueryable(statement.compoundSelectStmt!!)))
          .writeQueryInterfaces(file, output, backend)
        return@forEach
      }

      if (statement is SqlCreateVirtualTableStmt) return@forEach

      backend.writeTableInterface(packageName, query, statement, output)
    }
  }

  internal fun writeQueryInterfaces(
    file: SqlDelightQueriesFile,
    output: FileAppender,
    backend: Backend
  ) {
    file.namedQueries.writeQueryInterfaces(file, output, backend)
  }

  internal fun writeQueries(
    module: Module,
    dialect: SqlDelightDialect,
    file: SqlDelightQueriesFile,
    output: FileAppender,
    backend: Backend
  ) {
    val packageName = file.packageName ?: return
    backend.writeQueries(module, file, dialect, packageName, output)
  }

  private fun List<NamedQuery>.writeQueryInterfaces(file: SqlDelightFile, output: FileAppender, backend: Backend) {
    return filter { tryWithElement(it.select) { backend.needsInterface(it) } == true }
      .forEach { namedQuery -> backend.writeQueryInterface(namedQuery, file, output) }
  }
}

internal fun <T> tryWithElement(
  element: PsiElement,
  block: () -> T,
): T? {
  try {
    return block()
  } catch (e: ProcessCanceledException) {
    throw e
  } catch (e: InvalidElementDetectedException) {
    // It's okay if compilation is cut short, we can just quit out.
    return null
  } catch (e: Throwable) {
    val exception = IllegalStateException("Failed to compile $element :\n${element.text}")
    exception.initCause(e)
    throw exception
  }
}

