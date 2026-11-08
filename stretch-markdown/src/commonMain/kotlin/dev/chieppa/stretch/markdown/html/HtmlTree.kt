package dev.chieppa.stretch.markdown.html

import dev.chieppa.stretch.markdown.MarkdownBlock

/** A node of HTML read leniently, with markdown blocks that sit between its tags kept whole. */
internal sealed interface HtmlNode {
    class Element(val name: String, val attributes: Map<String, String>, val children: MutableList<HtmlNode> = ArrayList()) : HtmlNode

    data class Text(val text: String) : HtmlNode

    /** A markdown block between HTML tags, such as the text inside `<details>`, drawn as markdown. */
    data class Markdown(val block: MarkdownBlock) : HtmlNode
}

/** A run of a document: the tokens of an HTML block, or a markdown block. */
internal sealed interface HtmlPiece {
    data class Html(val tokens: List<HtmlToken>) : HtmlPiece

    data class Markdown(val block: MarkdownBlock) : HtmlPiece
}

/** Tags a new one of the same kind closes, as an unclosed `<p>` or `<li>` is closed by the next. */
private val closedBySibling = setOf("p", "li", "tr", "td", "th", "summary", "option")

/** Tags that hold items or rows: a sibling tag never closes one opened outside them. */
private val listTags = setOf("ul", "ol", "table", "details", "blockquote")

/** Tags a block-level tag closes, as a `<p>` left open ends where a list starts. */
private val closedByBlock = setOf("p")

/** Tags that start a block, closing an open `<p>`. */
internal val blockTags = setOf(
    "address", "article", "aside", "blockquote", "center", "details", "div", "dl", "fieldset", "figure", "footer",
    "form", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hr", "main", "nav", "ol", "p", "pre", "section", "summary",
    "table", "ul", "li", "dd", "dt", "tr", "td", "th", "thead", "tbody", "tfoot",
)

/**
 * Builds one tree from [pieces] in order, so a tag opened in one HTML block and closed in a later one holds the
 * markdown blocks between. Tags never closed end with the document; a closing tag with nothing open to match is dropped.
 */
internal fun htmlTree(pieces: List<HtmlPiece>): HtmlNode.Element {
    val root = HtmlNode.Element("#root", emptyMap())
    val open = ArrayList<HtmlNode.Element>().apply { add(root) }
    fun top() = open.last()
    fun closeTo(name: String): Boolean {
        val index = open.indexOfLast { it.name == name }
        if (index <= 0) return false
        while (open.size > index) open.removeAt(open.lastIndex)
        return true
    }
    for (piece in pieces) {
        when (piece) {
            is HtmlPiece.Markdown -> top().children += HtmlNode.Markdown(piece.block)
            is HtmlPiece.Html -> for (token in piece.tokens) {
                when (token) {
                    is HtmlToken.Text -> top().children += HtmlNode.Text(token.text)
                    is HtmlToken.Close -> closeTo(token.name)
                    is HtmlToken.Open -> {
                        if (token.name in closedBySibling) {
                            val same = open.indexOfLast { it.name == token.name }
                            val list = open.indexOfLast { it.name in listTags }
                            if (same > 0 && same > list) while (open.size > same) open.removeAt(open.lastIndex)
                        }
                        if (token.name in blockTags && top().name in closedByBlock) open.removeAt(open.lastIndex)
                        val element = HtmlNode.Element(token.name, token.attributes)
                        top().children += element
                        if (!token.selfClosing) open += element
                    }
                }
            }
        }
    }
    return root
}

/** All the text inside [node], as written. */
internal fun textOf(node: HtmlNode): String = when (node) {
    is HtmlNode.Text -> node.text
    is HtmlNode.Element -> node.children.joinToString("") { textOf(it) }
    is HtmlNode.Markdown -> node.block.source
}
