package  org.jetbrains.dokka.tc

import arrow.Kind
import arrow.fx.IO
import arrow.fx.extensions.io.concurrent.concurrent
import arrow.fx.extensions.io.unsafeRun.unsafeRun
import arrow.fx.typeclasses.Concurrent
import arrow.fx.typeclasses.UnsafeRun
import arrow.unsafe
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.ExtendingDSL
import org.jetbrains.dokka.postProcess.PostProcess

class TcPlugin : DokkaPlugin() {
  val typeCheck by extending {
    CoreExtensions.postProcess with typeChecking
  }
}

/**
 * [typeChecking] collects all code snippets in [SupportedMarkdownExtensions] and runs them in a ScriptEngine.
 * Where every compiler error is accumulated and displayed in the Console.
 */
val ExtendingDSL.typeChecking: PostProcess
  get() = postProcess("TypeChecking", with = IO.concurrent(), UR = IO.unsafeRun()) { config, ctx ->
    typeCheck(config, ctx, interpreter)
  }

/**
 * [postProcess] constructs a [PostProcess], where [run] is executed in a polymorphic Environment [F].
 */
fun <F> postProcess(
  name: String,
  with: Concurrent<F>,
  UR: UnsafeRun<F>,
  run: Concurrent<F>.(DokkaConfiguration, DokkaContext) -> Kind<F, Unit>
): PostProcess =
  object : PostProcess {
    override val name: String = name
    override suspend fun run(config: DokkaConfiguration, ctx: DokkaContext): Unit =
      UR.run {
        unsafe {
          runBlocking {
            run(with, config, ctx)
          }
        }
      }
  }