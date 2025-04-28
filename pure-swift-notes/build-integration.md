# Build Integration

## Command Line Interface

The CLI tool provides access to SQLDelight's Swift compiler:

```bash
sqldelight-swift-compiler \
    --source-folders src/main/sqldelight \
    --output-directory generated/swift \
    --package-name com.example.db
```

Implementation:
```kotlin
fun main(args: Array<String>) {
    val options = parseOptions(args)
    
    val environment = SqlDelightEnvironment(
        compilationUnit = SqlDelightCompilationUnit(
            sourceFolders = options.sourceFolders,
            outputDirectory = options.outputDirectory
        ),
        properties = SqlDelightDatabaseProperties(
            packageName = options.packageName
        ),
        moduleName = options.moduleName,
        compiler = SqlDelightSwiftCompiler()
    )

    val generationStatus = environment.generateSqlDelightFiles { info ->
        println(info)
    }

    when (generationStatus) {
        is Failure -> {
            System.err.println("Generation failed:")
            generationStatus.errors.forEach { System.err.println(it) }
            exitProcess(1)
        }
        is Success -> exitProcess(0)
    }
}
```

## Gradle Plugin

Extend existing plugin with explicit language configuration:

```kotlin
sqldelight {
    databases {
        create("MyDatabase") {
            // Either use legacy configuration (Kotlin only)
            packageName.set("com.example.db")
            sourceFolders.set(listOf("src/main/sqldelight"))
            
            // Or use explicit language configuration
            kotlin {
                packageName.set("com.example.db")
                outputDirectory.set(file("generated/kotlin"))
            }
            
            swift {
                packageName.set("MyApp")
                outputDirectory.set(file("generated/swift"))
            }
        }
    }
}
```

Note: Using legacy configuration with language-specific blocks will cause an error.

## Bazel Integration

Wrapper around CLI tool:

```python
# BUILD.bazel
load("@sqldelight//bazel:defs.bzl", "sqldelight_swift_library")

sqldelight_swift_library(
    name = "db",
    srcs = glob(["src/main/sqldelight/**/*.sq"]),
    package_name = "com.example.db",
)
```

Rule implementation:
```python
def _sqldelight_swift_library_impl(ctx):
    output_dir = ctx.actions.declare_directory(ctx.attr.name)
    
    args = ctx.actions.args()
    args.add("--source-folders", ctx.file.srcs.path)
    args.add("--output-directory", output_dir.path)
    args.add("--package-name", ctx.attr.package_name)
    
    ctx.actions.run(
        inputs = ctx.files.srcs,
        outputs = [output_dir],
        executable = ctx.executable._compiler,
        arguments = [args],
    )
    
    return [DefaultInfo(files = depset([output_dir]))]

sqldelight_swift_library = rule(
    implementation = _sqldelight_swift_library_impl,
    attrs = {
        "srcs": attr.label_list(allow_files = True),
        "package_name": attr.string(),
        "_compiler": attr.label(
            default = Label("@sqldelight//compiler:swift"),
            executable = True,
            cfg = "host",
        ),
    },
)