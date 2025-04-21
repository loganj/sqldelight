package app.cash.sqldelight.core.tables

import app.cash.sqldelight.core.compiler.swift.SwiftTableInterfaceGenerator
import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SwiftInterfaceGeneration {
  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test fun `sqlite primitives work`() {
    val result = FixtureCompiler.parseSql(
      """
      |CREATE TABLE test (
      |  an_integer INTEGER NOT NULL,
      |  a_real REAL NOT NULL,
      |  some_text TEXT NOT NULL,
      |  a_blob BLOB NOT NULL
      |);
      """.trimMargin(),
      tempFolder
    )

    val generator = SwiftTableInterfaceGenerator(result.sqlStatements().first().statement.createTableStmt!!.tableExposed())

    assertThat(generator.swiftImplementationSpec().toString()).isEqualTo(
      """
      |struct Test {
      |
      |  var an_integer: Swift.Int
      |  var a_real: Swift.Double
      |  var some_text: Swift.String
      |  var a_blob: Foundation.Data
      |
      |}
      |
      """.trimMargin(),
    )
  }
}
