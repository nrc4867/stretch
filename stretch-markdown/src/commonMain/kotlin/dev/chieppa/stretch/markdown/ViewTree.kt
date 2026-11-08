package dev.chieppa.stretch.markdown

import dev.chieppa.stretch.markdown.html.HtmlNode
import dev.chieppa.stretch.markdown.html.HtmlPiece
import dev.chieppa.stretch.markdown.html.blockTags
import dev.chieppa.stretch.markdown.html.htmlTokens
import dev.chieppa.stretch.markdown.html.htmlTree
import dev.chieppa.stretch.markdown.html.textOf

/** What a [MarkdownView] draws: markdown blocks as the editor draws them, and what HTML among them makes. */
internal sealed interface ViewNode {
    data class Block(val block: MarkdownBlock) : ViewNode

    /** A block of source that starts with an inline tag, such as `<b>`, read as a paragraph with HTML in it. */
    data class Inline(val block: MarkdownBlock) : ViewNode

    /** A paragraph, or a heading of [heading] 1 to 6. */
    class Text(val text: RichText, val heading: Int = 0) : ViewNode

    /** `<details>`: [summary] shows, and [children] open under it. */
    class Details(val summary: RichText, val children: List<ViewNode>) : ViewNode

    /** `<ul>` or `<ol>`, numbered from [start]. */
    class Items(val ordered: Boolean, val start: Int, val items: List<List<ViewNode>>) : ViewNode

    class Quote(val children: List<ViewNode>) : ViewNode

    class Pre(val code: String) : ViewNode

    data object Rule : ViewNode

    data class Image(val picture: Picture) : ViewNode

    /** `<table>`: its rows of cells, the first [headerRows] of them headers. */
    class Table(val rows: List<List<RichText>>, val headerRows: Int) : ViewNode
}

private val headingTags = (1..6).associateBy { "h$it" }
private val linkDefinitionLine = Regex("""^ {0,3}\[[^\]]+\]:\s*\S.*$""")

private val loneTagLine = Regex("""^ {0,3}</?[a-zA-Z][^>]*>\s*$""")
private val firstTagName = Regex("""^ {0,3}</?([a-zA-Z][a-zA-Z0-9-]*)""")

/** Whether [block] starts with HTML at all, rather than being front matter or indented code. */
private fun startsWithHtml(block: MarkdownBlock): Boolean = block.kind == BlockKind.Raw && htmlLine.containsMatchIn(block.source)

/**
 * Whether [block] is an HTML block as GitHub reads one: it starts with a comment, a block tag, or a line holding one
 * tag alone. A block starting with an inline tag and going on with text is a paragraph with HTML in it.
 */
internal fun isHtmlBlock(block: MarkdownBlock): Boolean {
    if (!startsWithHtml(block)) return false
    val first = block.source.substringBefore('\n')
    val name = firstTagName.find(first)?.groupValues?.get(1)?.lowercase() ?: return true
    return name in blockTags || loneTagLine.matches(first)
}

/** Whether [block] holds only link reference definitions, such as `[//]: # (note)`, which show nothing. */
internal fun isLinkDefinitions(block: MarkdownBlock): Boolean =
    block.kind == BlockKind.Paragraph && block.source.split('\n').all { linkDefinitionLine.matches(it) }

/** What [blocks] show as, with HTML read into what it draws and tags left open running on over the blocks after them. */
internal fun viewTree(blocks: List<MarkdownBlock>, styles: RichStyles): List<ViewNode> {
    val shown = blocks.filterNot(::isLinkDefinitions)
    if (shown.none(::isHtmlBlock)) return shown.map(::markdownNode)
    val pieces = shown.map { if (isHtmlBlock(it)) HtmlPiece.Html(htmlTokens(it.source)) else HtmlPiece.Markdown(it) }
    return HtmlBlocks(styles).of(htmlTree(pieces).children)
}

/** How a markdown [block] shows: as the editor draws it, or as a paragraph when it starts with an inline tag. */
private fun markdownNode(block: MarkdownBlock): ViewNode = if (startsWithHtml(block)) ViewNode.Inline(block) else ViewNode.Block(block)

/** Turns HTML nodes into [ViewNode]s, gathering inline content between blocks into paragraphs. */
private class HtmlBlocks(private val styles: RichStyles) {
    fun of(nodes: List<HtmlNode>): List<ViewNode> = buildList {
        val inline = ArrayList<HtmlNode>()
        fun flush() {
            if (inline.isEmpty()) return
            addAll(paragraph(inline))
            inline.clear()
        }
        for (node in nodes) {
            when {
                node is HtmlNode.Markdown -> {
                    flush()
                    add(markdownNode(node.block))
                }
                node is HtmlNode.Element && node.name in blockTags -> {
                    flush()
                    addAll(block(node))
                }
                else -> inline += node
            }
        }
        flush()
    }

    private fun inline(nodes: List<HtmlNode>): RichText = HtmlInlineText(styles).add(nodes).build()

    /** Inline content as a paragraph when it has any text, followed by its pictures. */
    private fun paragraph(nodes: List<HtmlNode>, heading: Int = 0): List<ViewNode> {
        val rich = inline(nodes)
        return buildList {
            if (rich.text.isNotBlank()) add(ViewNode.Text(rich, heading))
            rich.pictures.forEach { add(ViewNode.Image(it)) }
        }
    }

    private fun block(element: HtmlNode.Element): List<ViewNode> {
        val children = element.children
        headingTags[element.name]?.let { return paragraph(children, it) }
        return when (element.name) {
            "ul", "ol" -> listOf(items(element))
            "blockquote" -> listOf(ViewNode.Quote(of(children)))
            "pre" -> listOf(ViewNode.Pre(textOf(element).trim('\n')))
            "hr" -> listOf(ViewNode.Rule)
            "details" -> listOf(details(element))
            "table" -> listOf(table(element))
            "summary" -> paragraph(children)
            else -> of(children)
        }
    }

    private fun items(list: HtmlNode.Element): ViewNode.Items {
        val items = list.children.mapNotNull { child ->
            when {
                child is HtmlNode.Element && child.name == "li" -> of(child.children)
                child is HtmlNode.Text && child.text.isBlank() -> null
                else -> of(listOf(child)).ifEmpty { null }
            }
        }
        return ViewNode.Items(list.name == "ol", list.attributes["start"]?.toIntOrNull() ?: 1, items)
    }

    private fun details(element: HtmlNode.Element): ViewNode.Details {
        val summary = element.children.firstOrNull { it is HtmlNode.Element && it.name == "summary" } as HtmlNode.Element?
        val rest = element.children.filter { it !== summary }
        return ViewNode.Details(inline(summary?.children.orEmpty()), of(rest))
    }

    private fun table(element: HtmlNode.Element): ViewNode.Table {
        val rows = ArrayList<List<RichText>>()
        var headerRows = 0
        fun collect(node: HtmlNode.Element, inHead: Boolean) {
            for (child in node.children.filterIsInstance<HtmlNode.Element>()) {
                when (child.name) {
                    "tr" -> {
                        val cells = child.children.filterIsInstance<HtmlNode.Element>().filter { it.name == "td" || it.name == "th" }
                        if (inHead || (cells.isNotEmpty() && cells.all { it.name == "th" } && rows.size == headerRows)) headerRows++
                        rows += cells.map { inline(it.children) }
                    }
                    "thead" -> collect(child, inHead = true)
                    else -> collect(child, inHead)
                }
            }
        }
        collect(element, inHead = false)
        return ViewNode.Table(rows, headerRows)
    }
}
