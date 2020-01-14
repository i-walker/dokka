package org.jetbrains.dokka.tc

import arrow.core.Some
import arrow.core.Tuple2
import arrow.core.Tuple3
import arrow.core.toT
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

sealed class SnippetParserState {
  data class CollectingCode(val snippet: Snippet) : SnippetParserState()
  object Searching : SnippetParserState()
}

val interpreter: TcOps =
  object : TcOps {
    private fun Path.containsSnippets(): Boolean =
      toFile().bufferedReader().use {
        it.lines().anyMatch { s -> s.contains("```kotlin") }
      }

    override suspend fun Path.files(): List<TcProcessingContext> =
      Files.walk(this)
        .filter { Files.isDirectory(it).not() }
        .filter { path ->
          SupportedMarkdownExtensions.fold(false) { c, ext ->
            c || path.toString().endsWith(ext)
          } && path.containsSnippets()
        }.iterator().asSequence().mapIndexed { index, path ->
          TcProcessingContext(index, path)
        }.toList()

    override suspend fun targetDir(config: DokkaConfiguration): Path =
      Paths.get(config.outputDir)

    override suspend fun <R> Path.foldLines(f: (Sequence<String>) -> R): R =
      toFile().useLines { f(it) }

    override fun compilerArgs(ctx: DokkaContext, config: DokkaConfiguration): List<URL> =
      config.passesConfigurations.map {
        it.runtimeClassPath.map { f -> File(f.path).toURI().toURL() }
      }.flatten().distinct()

    val fenceRegexStart = "```(.*)".toRegex()
    val fenceRegexEnd = "```.*".toRegex()

    override fun extractCode(content: Sequence<String>): Tuple2<List<String>, List<Snippet>> {
      val result: Tuple3<SnippetParserState, List<String>, List<Snippet>> =
        content.fold(
          Tuple3(
            SnippetParserState.Searching as SnippetParserState,
            emptyList(),
            emptyList()
          )
        ) { (state: SnippetParserState, lines, snippets), line ->
          when (state) {
            is SnippetParserState.Searching -> {
              val startMatch = fenceRegexStart.matchEntire(line)
              if (startMatch != null) { // found a fence start
                val lang = startMatch.groupValues[1].trim()
                val snippet = Snippet(line, lang, "")
                Tuple3(SnippetParserState.CollectingCode(snippet), lines + line, snippets)
              } else Tuple3(state, lines + line, snippets) // we are still searching
            }
            is SnippetParserState.CollectingCode -> {
              val endMatch = fenceRegexEnd.matchEntire(line)
              if (endMatch != null) { // found a fence end
                Tuple3(
                  SnippetParserState.Searching,
                  lines + line,
                  snippets + state.snippet.copy(fence = state.snippet.fence + "\n" + line)
                )
              } else { // accumulating code inside a fence
                val modifiedSnippet = state.snippet.copy(
                  fence = state.snippet.fence + "\n" + line,
                  code = state.snippet.code + "\n" + line
                )
                Tuple3(state.copy(snippet = modifiedSnippet), lines + line, snippets)
              }
            }
          }
        }
      return result.b toT result.c
    }

    override suspend fun compileCode(
      snippets: Tuple2<Path, List<Snippet>>,
      compilerArgs: List<URL>
    ): List<Snippet> =
      getEngineCache(snippets.b, compilerArgs).let { engineCache ->
        // run each snipped and handle its result
        snippets.b.mapIndexed { i, snip ->
          try {
            val result = engineCache[snip.lang]?.eval(snip.code)
            snip.copy(result = Some("// $result"))
          } catch (error: Throwable) {
            printConsole(colored(ANSI_RED, "[✗ ${snippets.a} [${i + 1}]"))
            throw CompilationException(
              snippets.a, snip, error, msg = "\n" +
                """
                | File located at: ${snippets.a}
                |
                |```
                |${snip.code}
                |```
                |${colored(ANSI_RED, error.localizedMessage)}
                """.trimMargin()
            )
          }
        }
      }

    override suspend fun printConsole(msg: String): Unit = println(msg)

    private val engineCache: ConcurrentMap<List<URL>, Map<String, ScriptEngine>> = ConcurrentHashMap()

    private fun getEngineCache(
      snippets: List<Snippet>,
      compilerArgs: List<URL>
    ): Map<String, ScriptEngine> {
      val cache = engineCache[compilerArgs]
      return if (cache == null) { // create a new engine
        val classLoader = URLClassLoader(compilerArgs.toTypedArray())
        val seManager = ScriptEngineManager(classLoader)
        val langs: List<String> = snippets.map { it.lang }.distinct()
        val engines: Map<String, ScriptEngine> = langs.toList().map {
          it to seManager.getEngineByExtension(extensionMappings.getOrDefault(it, "kts"))
        }.toMap()
          .filterValues { it != null }
        engineCache.putIfAbsent(compilerArgs, engines) ?: engines
      } else { // reset an engine. Non thread-safe
        cache.forEach { (_, engine) ->
          engine.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE)
        }
        cache
      }
    }
  }