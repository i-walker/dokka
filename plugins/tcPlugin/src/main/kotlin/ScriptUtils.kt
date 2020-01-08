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
}