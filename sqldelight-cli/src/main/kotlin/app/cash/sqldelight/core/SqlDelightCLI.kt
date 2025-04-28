
package app.cash.sqldelight.core

import app.cash.sqldelight.core.SqlDelightEnvironment.CompilationStatus.Failure
import app.cash.sqldelight.core.compiler.SqlDelightCompiler
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import java.io.File
import java.nio.file.Paths
import java.util.ServiceLoader
import java.util.logging.Logger

object SqlDelightCLI {

  private const val PACKAGE_NAME_FLAG = "--package-name="
  private const val DATABASE_NAME_FLAG = "--database-name="
  private const val MODULE_NAME_FLAG = "--module-name="
  private const val OUTPUT_DIRECTORY_FLAG = "--output-directory="
  private const val SOURCE_DIRECTORIES_FLAG = "--source-directories="
  private const val DIALECT_FLAG = "--dialect="
  private const val DESCRIPTOR_FILE_FLAG = "--descriptor-directory="

  @JvmStatic
  fun main(args: Array<String>) {

    val arguments = args.parseArguments()

    val dialect = ServiceLoader.load(SqlDelightDialect::class.java).first { dialect ->
      dialect::class.java.name == arguments.dialectClassName
    }

    arguments.descriptorFile.parentFile?.mkdirs()

    val environment = SqlDelightEnvironment(
      properties = object : SqlDelightDatabaseProperties {
        override val packageName = arguments.packageName
        override val className = arguments.databaseName
        override val dependencies = emptyList<SqlDelightDatabaseName>()
        override val compilationUnits = emptyList<SqlDelightCompilationUnit>()
        override val deriveSchemaFromMigrations = false
        override val treatNullAsUnknownForEquality = false
        override val rootDirectory = Paths.get("").toFile()
        override val generateAsync: Boolean = false
      },
      compilationUnit = CliCompilationUnit(
        name = arguments.moduleName,
        sourceFolders = arguments.sourceDirectories.map { CliSourceFolder(it, false) },
        outputDirectoryFile = arguments.outputDirectory
      ),
      verifyMigrations = false,
      dialect = dialect,
      moduleName = arguments.moduleName
    )

    val logger = Logger.getLogger(SqlDelightCompiler::class.java.name)
    val status: SqlDelightEnvironment.CompilationStatus = environment.generateSqlDelightFiles(logger::info)
    if (status is Failure) {
      status.errors.forEach { logger.severe(it) }
      throw SqlDelightException(
        message = "Compilation failed, see error output.",
      )
    }
  }

  private data class CliCompilationUnit(
    override val name: String,
    override val sourceFolders: List<SqlDelightSourceFolder>,
    override val outputDirectoryFile: File
  ) : SqlDelightCompilationUnit

  private data class CliSourceFolder(
    override val folder: File,
    override val dependency: Boolean,
  ) : SqlDelightSourceFolder

  private data class Args(
    val packageName: String,
    val databaseName: String,
    val moduleName: String,
    val outputDirectory: File,
    val sourceDirectories: List<File>,
    val dialectClassName: String,
    val descriptorFile: File
  )

  private fun Array<String>.parseArguments(): Args {

    lateinit var packageName: String
    lateinit var databaseName: String
    lateinit var moduleName: String
    lateinit var outputDirectory: File
    lateinit var sourceDirectories: List<File>
    var dialect = "sqlite_3_18"
    var descriptorFilePath: String? = null

    for (arg in this) {
      when {
        arg.startsWith(PACKAGE_NAME_FLAG) ->
          packageName = arg.substring(PACKAGE_NAME_FLAG.length)
        arg.startsWith(DATABASE_NAME_FLAG) ->
          databaseName = arg.substring(DATABASE_NAME_FLAG.length)
        arg.startsWith(MODULE_NAME_FLAG) ->
          moduleName = arg.substring(MODULE_NAME_FLAG.length)
        arg.startsWith(OUTPUT_DIRECTORY_FLAG) ->
          outputDirectory = File(arg.substring(OUTPUT_DIRECTORY_FLAG.length))
        arg.startsWith(SOURCE_DIRECTORIES_FLAG) ->
          sourceDirectories = arg.substring(SOURCE_DIRECTORIES_FLAG.length).split(',').map { File(it) }
        arg.startsWith(DIALECT_FLAG) ->
          dialect = arg.substring(DIALECT_FLAG.length)
        arg.startsWith(DESCRIPTOR_FILE_FLAG) ->
          descriptorFilePath = arg.substring(DESCRIPTOR_FILE_FLAG.length)
      }
    }

    val descriptorFile: File = if (descriptorFilePath == null) {
      File(outputDirectory, "descriptor.json")
    } else {
      File(descriptorFilePath)
    }

    return Args(
      packageName = packageName,
      databaseName = databaseName,
      moduleName = moduleName,
      outputDirectory = outputDirectory,
      sourceDirectories = sourceDirectories,
      dialectClassName = "app.cash.sqldelight.dialects.${dialect}.SqliteDialect",
      descriptorFile = descriptorFile
    )
  }
}
