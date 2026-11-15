package dev.chieppa.stretch.markdown

import androidx.compose.ui.text.SpanStyle
import dev.chieppa.stretch.markdown.html.HtmlToken
import dev.chieppa.stretch.markdown.html.htmlTokens

private val plain: RichStyles = { SpanStyle() }
private val spaces = Regex("""\s+""")

/**
 * The first line of [markdown] that shows any text, as plain text a reader would see in a [MarkdownView]: markers,
 * tags, pictures and comments left out, references and shortcodes decoded. For one-line previews such as a folded
 * comment. Empty when nothing in it shows text.
 */
fun markdownPeek(markdown: String): String {
    for (block in MarkdownDocument.parse(markdown).blocks) {
        if (isLinkDefinitions(block)) continue
        val shown = if (isHtmlBlock(block)) {
            htmlTokens(block.source).filterIsInstance<HtmlToken.Text>().joinToString("") { replaceShortcodes(it.text) }
        } else {
            readOnlyText(block.source, if (startsWithHtml(block)) BlockKind.Paragraph else block.kind, plain).text.text
        }
        val line = shown.lineSequence().map { it.replace(spaces, " ").trim() }.firstOrNull { it.isNotEmpty() }
        if (line != null) return line
    }
    return ""
}
