/*
package org.jetbrains.dokka.tc

import io.github.classgraph.ClassGraph
import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

@KotlinScript(fileExtension = "dokka.kts")
interface ScriptUtils

fun evaluate(snip: String, classpath: List<File> = ClassGraph().classpathFiles): ResultWithDiagnostics<EvaluationResult> =
  BasicJvmScriptingHost().eval(
    script = snip.toScriptSource(),
    compilationConfiguration = createJvmCompilationConfigurationFromTemplate<ScriptUtils> {
      jvm {
        updateClasspath(
          scriptCompilationClasspathFromContext(classLoader = this::class.java.classLoader,wholeClasspath = true).filterNot { it.name.contains("stdlib") }
            .plus(classpath.filterNot { it.path.contains("kotlin-compiler-[\\d.*].*.jar".toRegex()) })
            .distinct()
            .also {
              println("classpath: ${it.size}: $it")
            }
        )
      }
    },
    evaluationConfiguration = ScriptEvaluationConfiguration {
      jvm
    }
  )

fun main() {
  val works = // compiles fine
    evaluate(
      """
      import org.jetbrains.dokka.tc.ScriptUtils
      import arrow.core.Id
      import java.io.File
      
      val s: Id<String> = Id("4")
      val d = ScriptUtils::class.java
      "hello"
      s.extract()
    """
    )
  println(works)
}*/

/*class ArrowJvmLocalScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {

  val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<ScriptUtils> {
    jvm {
      updateClasspath(
        scriptCompilationClasspathFromContext(
          classLoader = this.javaClass.classLoader,
          wholeClasspath = true
        ).distinct()
      )
    }
  }
  val evaluationConfiguration = createJvmEvaluationConfigurationFromTemplate<ScriptUtils> {
    jvm
  }
  var lastClassLoader: ClassLoader? = null
  var lastClassPath: List<File> = emptyList()

  override fun getScriptEngine(): ScriptEngine =
    KotlinJsr223ScriptEngineImpl(
      this,
      ScriptCompilationConfiguration(compilationConfiguration) {
        jvm {
          dependenciesFromCurrentContext()

        }
      }.withUpdatedClasspath(ClassGraph().classpathFiles),
      evaluationConfiguration
    ) { ScriptArgsWithTypes(arrayOf(it.getBindings(ScriptContext.ENGINE_SCOPE).orEmpty()), arrayOf(Bindings::class)) }

  fun JvmScriptCompilationConfigurationBuilder.dependenciesFromCurrentContext() {
    val currentClassLoader = Thread.currentThread().contextClassLoader
    val classPath = if (lastClassLoader == null || lastClassLoader != currentClassLoader) {
      scriptCompilationClasspathFromContext(
        classLoader = currentClassLoader,
        wholeClasspath = true,
        unpackJarCollections = true
      ).also {
        lastClassLoader = currentClassLoader
        lastClassPath = it
      }
    } else lastClassPath
    updateClasspath(classPath)
  }

  override fun getExtensions(): List<String> = listOf("arrow.kts")
}*/

/*snippets.b.mapIndexed { i, snip ->
      //val pre = evaluate(snip.code)
      val result = evaluate(snip.code, compilerArgs).valueOr {
        error ->
        printConsole(colored(ANSI_RED, "[✗ ${snippets.a} [${i + 1}]"))
        val exception = DiagnosticException(error.reports)
        throw CompilationException(
          snippets.a, snip, exception, msg = "\n" +
            """
              | File located at: ${snippets.a}
              |
              |```
              |${snip.code}
              |```
              |${colored(ANSI_RED, exception.msg)}
              """.trimMargin()
        )
      }
      snip.copy(result = Some("// $result"))




data class DiagnosticException(
  val errors: List<ScriptDiagnostic>,
  val msg: String = errors.joinToString(separator = "\n") { "$it" }
) : NoStackTrace(msg)
    }*/

/*val engine = ScriptEngineManager().getEngineByExtension("kts")!!
val script = """
    import arrow.core.Id
    import java.io.File
    import arrow.fx.IO

    val s: Id<String> = Id("4")
    "hello"
    s.extract()
    IO::class.java
  """
val result = engine.eval(script)
println(result)
val result = ((Thread.currentThread().contextClassLoader as URLClassLoader).urLs ?: emptyArray<URL>()).toList()
        .filterNot { it.path.contains("Java/JavaVirtualMachines") }
        .filterNot { it.path.contains("JetBrains/Toolbox/")  }
        .map { Paths.get(it.toURI()).toFile() }
    result.forEach {
        println(it.path)
    }
*/