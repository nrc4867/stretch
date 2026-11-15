package dev.chieppa.stretch.markdown

import androidx.compose.ui.text.AnnotatedString
import dev.chieppa.stretch.markdown.html.HtmlNode

/** Inline tags and the style each gives its text. */
private val inlineStyles = mapOf(
    "strong" to RichStyle.Bold, "b" to RichStyle.Bold,
    "em" to RichStyle.Italic, "i" to RichStyle.Italic, "cite" to RichStyle.Italic,
    "code" to RichStyle.Code, "kbd" to RichStyle.Code, "tt" to RichStyle.Code, "samp" to RichStyle.Code,
    "s" to RichStyle.Strike, "del" to RichStyle.Strike, "strike" to RichStyle.Strike,
    "sub" to RichStyle.Sub, "sup" to RichStyle.Sup,
)

/**
 * HTML's inline content as text: runs of white space become one space, as a browser shows them, `<br>` breaks the
 * line, styled tags style their text, `<a href>` links it, and `<img>` becomes a [Picture] shown after the text.
 * Tags it does not know keep their text.
 */
internal class HtmlInlineText(private val styles: RichStyles) {
    private val builder = AnnotatedString.Builder()
    private val pictures = ArrayList<Picture>()
    private val places = ArrayList<Int>()
    private var space = false
    private var lineStart = true
    private var link: String? = null

    fun add(nodes: List<HtmlNode>): HtmlInlineText = apply { nodes.forEach(::add) }

    fun build(): RichText = RichText(builder.toAnnotatedString(), pictures, places)

    private fun add(node: HtmlNode) {
        when (node) {
            is HtmlNode.Text -> text(replaceShortcodes(node.text))
            is HtmlNode.Markdown -> markdown(node.block)
            is HtmlNode.Element -> element(node)
        }
    }

    private fun text(text: String) {
        for (char in text) {
            if (char == ' ' || char == '\t' || char == '\n' || char == '\r') {
                space = !lineStart
                continue
            }
            if (space) builder.append(' ')
            space = false
            lineStart = false
            builder.append(char)
        }
    }

    private fun element(element: HtmlNode.Element) {
        val children = element.children
        when (element.name) {
            "br" -> {
                builder.append('\n')
                space = false
                lineStart = true
            }
            "img" -> element.attributes["src"]?.takeIf { it.isNotBlank() }?.let { src ->
                pictures += Picture(src, element.attributes["alt"].orEmpty(), element.attributes["width"]?.toIntOrNull(), link)
                if (space) builder.append(' ')
                space = false
                lineStart = false
                places += builder.length
            }
            "a" -> {
                val href = element.attributes["href"]
                if (href.isNullOrBlank()) {
                    add(children)
                    return
                }
                val outer = link
                link = href
                builder.pushStringAnnotation(URL_ANNOTATION, href)
                builder.pushStyle(styles(RichStyle.Link))
                add(children)
                builder.pop()
                builder.pop()
                link = outer
            }
            else -> {
                val style = inlineStyles[element.name]
                if (style == null) {
                    add(children)
                } else {
                    builder.pushStyle(styles(style))
                    if (style == RichStyle.Code) builder.pushStringAnnotation(CODE_ANNOTATION, "")
                    add(children)
                    if (style == RichStyle.Code) builder.pop()
                    builder.pop()
                }
            }
        }
    }

    /** A markdown block inside inline HTML, such as a paragraph inside `<summary>`, read as its text shows. */
    private fun markdown(block: MarkdownBlock) {
        val read = readOnlyText(block.source, block.kind, styles)
        if (read.text.isEmpty() && read.pictures.isEmpty()) return
        if (space) builder.append(' ')
        space = false
        lineStart = false
        val from = builder.length
        builder.append(read.text)
        pictures += read.pictures
        places += read.places.map { from + it }
    }
}
