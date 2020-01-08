package org.jetbrains.dokka.tc

import arrow.Kind
import arrow.core.Eval
import arrow.core.ListK
import arrow.core.Nel
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.combine
import arrow.core.extensions.list.foldable.reduceLeftOption
import arrow.core.extensions.listk.semigroup.semigroup
import arrow.core.extensions.nonemptylist.semigroup.semigroup
import arrow.core.getOrElse
import arrow.core.k
import arrow.core.validNel
import arrow.fx.typeclasses.Async
import arrow.typeclasses.Applicative
import io.github.classgraph.ClassGraph
import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Paths
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.jvm.JvmScriptCompilationConfigurationBuilder
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext
import kotlin.script.experimental.jvm.withUpdatedClasspath
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl

class ArrowJvmLocalScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {

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
}

const val ANSI_RESET = "\u001B[0m"
const val ANSI_GREEN = "\u001B[32m"
const val ANSI_RED = "\u001B[31m"

val classpath: List<File>
  get() = ((Thread.currentThread().contextClassLoader as URLClassLoader).urLs ?: emptyArray<URL>()).toList()
    .filterNot { it.path.contains("Java/JavaVirtualMachines") }
    .filterNot { it.path.contains("JetBrains/Toolbox/") }
    .map { Paths.get(it.toURI()).toFile() }

val classgraphR: List<File>
  get() = ClassGraph().classpathURIs.filterNotNull().map { Paths.get(it).toFile() }

fun colored(color: String, message: String) =
  "$color$message$ANSI_RESET"

fun <A, B, G> List<A>.traverseIndexed(GA: Applicative<G>, f: (Int, A) -> Kind<G, B>): Kind<G, ListK<B>> =
  foldRightIndexed<A, Eval<Kind<G, ListK<B>>>>(Eval.always { GA.just(emptyList<B>().k()) }) { i, a, eval ->
    GA.run { f(i, a).map2Eval(eval) { (listOf(it.a) + it.b).k() } }
  }.value()

fun <A, EE, F> List<Kind<F, ValidatedNel<EE, A>>>.collectErrors(AF: Async<F>): Kind<F, Kind<F, ValidatedNel<EE, List<A>>>> =
  AF.run {
    effect {
      map { fa ->
        fa.map { validated: ValidatedNel<EE, A> ->
          validated.map { a: A -> ListK.just(a) }
        }
      }.reduceLeftOption { fa, fb ->
        fa.map2(fb) { (a: Validated<Nel<EE>, ListK<A>>, b: Validated<Nel<EE>, ListK<A>>) ->
          a.combine(Nel.semigroup(), ListK.semigroup(), b)
        }
      }.getOrElse { just(ListK.empty<A>().validNel()) }
    }
  }

fun toUrl(f: File): URL = f.toURI().toURL()
