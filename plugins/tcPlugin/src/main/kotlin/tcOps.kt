package org.jetbrains.dokka.tc

import arrow.core.None
import arrow.core.Option
import arrow.core.Tuple2
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File
import java.nio.file.Path

data class TcProcessingContext(
  val index: Int,
  val path: Path
)

interface TcOps {
  suspend fun Path.files(): List<TcProcessingContext>

  suspend fun targetDir(config: DokkaConfiguration): Path

  suspend fun <R> Path.foldLines(f: (Sequence<String>) -> R): R

  fun compilerArgs(ctx: DokkaContext, config: DokkaConfiguration): List<File>

  fun extractCode(content: Sequence<String>): Tuple2<List<String>, List<Snippet>>

  suspend fun compileCode(
    snippets: Tuple2<Path, List<Snippet>>,
    compilerArgs: List<File>
  ): List<Snippet>

  suspend fun printConsole(msg: String): Unit
}

val extensionMappings = mapOf(
  "kotlin" to "kts"
)

val SupportedMarkdownExtensions: Set<String> = setOf(
  "markdown",
  "mdown",
  "mkdn",
  "md",
  "mkd",
  "mdwn",
  "mdtxt",
  "mdtext",
  "text",
  "Rmd"
)

data class Snippet(
  val fence: String,
  val lang: String,
  val code: String,
  val result: Option<String> = None
)


data class TcFailedException(val msg: String) : NoStackTrace(msg) {
  override fun toString(): String = msg
}

abstract class NoStackTrace(msg: String) : Throwable(msg, null, false, false)

data class CompilationException(
  val path: Path,
  val snippet: Snippet,
  val underlying: Throwable,
  val msg: String
) : NoStackTrace(msg) {
  override fun toString(): String = msg
}
