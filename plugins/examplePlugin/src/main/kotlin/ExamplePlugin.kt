package  org.jetbrains.dokka.tc

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.postProcess.PostProcess
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import javax.script.ScriptEngineManager

class ExamplePlugin : DokkaPlugin() {
  val runScriptEngine by extending {
    CoreExtensions.postProcess with runEngine
  }
}

/*
 * runs the ScriptEngine and tries to typeCheck and evaluate code
 */
val runEngine: PostProcess
  get() = postProcess("ScriptEngine Example") { config, _ ->
    val classLoader = URLClassLoader(config.compilerArgs().toTypedArray())
    val seManager = ScriptEngineManager(classLoader)
    val result = seManager.getEngineByExtension("kts").eval(
      """
        val x: Int = 54
        x
      """.trimIndent()
    )
    println(result)
  }

fun postProcess(
  name: String,
  run: (DokkaConfiguration, DokkaContext) -> Unit
): PostProcess =
  object : PostProcess {
    override val name: String = name
    override suspend fun run(config: DokkaConfiguration, ctx: DokkaContext): Unit =
      run(config, ctx)
  }

fun DokkaConfiguration.compilerArgs(): List<URL> =
  passesConfigurations.map {
    it.runtimeClassPath.map { f -> File(f.path).toURI().toURL() }
  }.flatten().distinct()

val d = ExamplePlugin::class.java.simpleName.orEmpty()

fun main() { // running main will resolves fine with dependencies from the classpath
  val seManager = ScriptEngineManager()
  val result = seManager.getEngineByExtension("kts").eval(
    """
        import org.jetbrains.dokka.tc.ExamplePlugin
        import org.jetbrains.dokka.tc.d
        
        val x: Int = 54
        x
        d
      """.trimIndent()
  )
  println(result)
}