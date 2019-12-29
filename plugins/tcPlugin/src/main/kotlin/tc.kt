package org.jetbrains.dokka.tc

import arrow.Kind
import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.fix
import arrow.core.toT
import arrow.fx.typeclasses.Concurrent
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.plugability.DokkaContext
import kotlin.math.ln
import kotlin.math.pow


fun Long.humanBytes(): String {
  val unit = 1024
  if (this < unit) return toString() + " B"
  val exp = (ln(toDouble()) / ln(unit.toDouble())).toInt()
  val pre = ("KMGTPE")[exp - 1] + "i"
  return String.format("%.1f %sB", this / unit.toDouble().pow(exp.toDouble()), pre)
}

fun <A> toValidatedNel(a: Either<Throwable, A>): ValidatedNel<Throwable, A> =
  a.fold({ e -> Validated.Invalid(NonEmptyList(e)) }, { a -> Validated.Valid(a) })

fun <F> Concurrent<F>.typeCheck(config: DokkaConfiguration, ctx: DokkaContext, tcOps: TcOps): Kind<F, Unit> =
  with(tcOps) {
    fx.concurrent {
      !effect { printConsole("TypeCheck Plugin is running") }
      val heapSize = Runtime.getRuntime().totalMemory()
      val heapMaxSize = Runtime.getRuntime().maxMemory()
      !effect { printConsole("Current heap used: ${(heapSize - Runtime.getRuntime().freeMemory()).humanBytes()}") }
      !effect { printConsole("Starting with Heap Size: ${heapSize.humanBytes()}, Max Heap Size: ${heapMaxSize.humanBytes()}") }
      val path = !effect { targetDir(config) }
      val validatedPaths = !effect {
        path.files().map { (index, p) ->
          effect {
            // collecting and type checking each snippet
            val totalHeap = Runtime.getRuntime().totalMemory()
            val usedHeap = totalHeap - Runtime.getRuntime().freeMemory()
            val message =
              "TypeChecking Compiles: [$index] ${path.relativize(p)} | Used Heap: ${usedHeap.humanBytes()}"
            printConsole(colored(ANSI_GREEN, message))
            val (_, snippets) = p.foldLines(::extractCode)
            val compilerArgs = compilerArgs(ctx, config)
            compileCode(p toT snippets, compilerArgs)
            p
          }.attempt().map(::toValidatedNel).map { it.fix() }
        }
      }

      val combinedResults = !!validatedPaths.collectErrors(this@typeCheck)

      !effect {
        combinedResults.fold({ errors ->
          val separator = "\n----------------------------------------------------------------\n"
          throw TcFailedException(errors.all
            .flatMap {
              if (it is CompilationException) listOf(it)
              else emptyList()
            }.joinToString(prefix = separator, separator = separator) {
              it.msg
            }
          )
        }, { paths ->
          val message = colored(ANSI_GREEN, "TypeChecking processed ${paths.size} files")
          printConsole(message)
        })
      }
    }
  }

