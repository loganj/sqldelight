package app.cash.sqldelight.core.compiler

import app.cash.sqldelight.core.SqlDelightDatabaseName
import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.compiler.kotlin.DatabaseExposerGenerator
import app.cash.sqldelight.core.compiler.kotlin.DatabaseGenerator
import app.cash.sqldelight.core.compiler.kotlin.QueriesTypeGenerator
import app.cash.sqldelight.core.compiler.kotlin.QueryInterfaceGenerator
import app.cash.sqldelight.core.compiler.kotlin.TableInterfaceGenerator
import app.cash.sqldelight.core.compiler.kotlin.getInterfaceType
import app.cash.sqldelight.core.compiler.model.NamedQuery
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.core.lang.queriesName
import app.cash.sqldelight.core.lang.util.sqFile
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.SqlCreateVirtualTableStmt
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.NameAllocator
import java.io.Closeable

object KotlinBackend : Backend() {

  override fun writeTableInterface(
    packageName: String,
    query: LazyQuery,
    statement: PsiElement,
    output: (fileName: String) -> Appendable,
  ) {
    val fileSpec = FileSpec.builder(packageName, allocateName(query.tableName))
      .apply {
        tryWithElement(statement) {
          val generator = TableInterfaceGenerator(query)
          addType(generator.kotlinImplementationSpec())
        }
      }
      .build()

    statement.sqFile().generatedDirectories?.forEach { directory ->
      fileSpec.writeToAndClose(output("$directory/${allocateName(query.tableName).capitalize()}.kt"))
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
    val queryWrapperType = DatabaseGenerator(module, file).interfaceType()
    val fileSpec = FileSpec.builder(packageName, queryWrapperType.name!!)
      // TODO: Remove these when kotlinpoet supports top level types.
      .addImport("$packageName.$implementationFolder", "newInstance", "schema")
      .apply {
        var index = 0
        dependencies.forEach {
          addAliasedImport(ClassName(it.packageName, it.className), "${it.className}${index++}")
        }
      }
      .addType(queryWrapperType)
      .build()
    outputPaths.forEach { outputDirectory ->
      val packageDirectory = "$outputDirectory/${packageName.replace(".", "/")}"
      fileSpec.writeToAndClose(output("$packageDirectory/${queryWrapperType.name}.kt"))
    }
  }

  override fun needsInterface(query: NamedQuery): Boolean {
    val needsWrapper = needsWrapper(query)
    val pureTable = query.pureTable
    val parent = pureTable?.parent

    return needsWrapper && (pureTable == null || parent is SqlCreateVirtualTableStmt)
  }

  override fun needsWrapper(query: NamedQuery): Boolean = (query.resultColumns.size > 1 || query.resultColumns[0].javaType.isNullable)

  override fun writeQueryInterface(
    namedQuery: NamedQuery,
    file: SqlDelightFile,
    output: (fileName: String) -> Appendable,
  ) {
    val packageName = namedQuery.getInterfaceType().packageName
    val fileSpec = FileSpec.builder(packageName, namedQuery.name)
      .apply {
        tryWithElement(namedQuery.select) {
          val generator = QueryInterfaceGenerator(namedQuery)
          addType(generator.kotlinImplementationSpec())
        }
      }
      .build()

    file.generatedDirectories(packageName)?.forEach { directory ->
      fileSpec.writeToAndClose(output("$directory/${namedQuery.name.capitalize()}.kt"))
    }
  }

  override fun writeQueries(
    module: Module,
    file: SqlDelightQueriesFile,
    dialect: SqlDelightDialect,
    packageName: String,
    output: (fileName: String) -> Appendable,
  ) {
    val queriesType = QueriesTypeGenerator(module, file, dialect)
      .generateType(packageName) ?: return

    val fileSpec = FileSpec.builder(packageName, file.queriesName.capitalize())
      .addType(queriesType)
      .build()

    file.generatedDirectories?.forEach { directory ->
      fileSpec.writeToAndClose(output("$directory/${queriesType.name}.kt"))
    }
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
    val databaseImplementationType = DatabaseGenerator(module, sourceFile).type()
    val exposer = DatabaseExposerGenerator(
      databaseImplementationType,
      packageName,
      className,
      generateAsync = sourceFile.generateAsync,
    )

    val implPackageName = "$packageName.$implementationFolder"
    val fileSpec = FileSpec.builder(implPackageName, databaseImplementationType.name!!)
      .addProperty(exposer.exposedSchema())
      .addFunction(exposer.exposedConstructor())
      .addType(databaseImplementationType)
      .build()

    outputDirectories.forEach { outputDirectory ->
      val packageDirectory = "$outputDirectory/${implPackageName.replace(".", "/")}"
      fileSpec.writeToAndClose(output("$packageDirectory/${databaseImplementationType.name}.kt"))
    }
  }

  override fun allocateName(namedElement: NamedElement): String {
    return NameAllocator().newName(namedElement.normalizedName)
  }
}

private fun FileSpec.writeToAndClose(appendable: Appendable) {
  writeTo(appendable)
  if (appendable is Closeable) appendable.close()
}
