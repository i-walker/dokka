package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.resolvers.LocationProvider

open class MDRenderer(
    fileWriter: FileWriter,
    locationProvider: LocationProvider,
    context: DokkaContext
) : DefaultRenderer(fileWriter, locationProvider, context) {

    override fun buildCode(code: List<ContentNode>, language: String, pageContext: PageNode): String =
        """```$language
            |${code.joinToString(separator = "\n") { it.build(pageContext) }}
            |```
            |
        """.trimMargin()

    override fun buildHeader(level: Int, text: String): String = "${"#".repeat(level)}$text\n"

    override fun buildLink(text: String, address: String): String = "[$text]($address)"

    private fun buildListElement(items: List<ContentNode>, ctx: PageNode, ordered: Boolean, depth: Int): String =
        items.foldIndexed("") { index, acc, node ->
            acc + when (node) {
                is ContentText -> "${" ".repeat(depth)}${if (ordered) "${index + 1}." else "*"} ${node.build(ctx)}\n"
                is ContentList -> buildListElement(node.children, ctx, node.ordered, depth + 2)
                else -> ""
            }
        }

    override fun buildList(node: ContentList, pageContext: PageNode): String =
        buildListElement(node.children, pageContext, node.ordered, 0)

    override fun buildNewLine(): String = "\n"

    override fun buildResource(node: ContentEmbeddedResource, pageContext: PageNode): String =
        "![${node.altText}](${node.address})"

    override fun buildTable(node: ContentTable, pageContext: PageNode): String =
        "${node.header.joinToString(separator = " | ") { it.build(pageContext) }}\n" +
                List(node.header.size) { "---" }.joinToString(separator = " | ")
    
}