package dev.chieppa.stretch.markdown

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

enum class InlineStyle { Bold, Italic, Strike, Code, Link }

/** Characters [start] to [end] have [style]. A [Link] span carries where it goes in [url]. */
data class InlineSpan(val start: Int, val end: Int, val style: InlineStyle, val url: String? = null)

/** The formatting within some inline text: what is styled, and the marker characters people need not see. */
data class InlineMarkup(val spans: List<InlineSpan>, val markers: List<IntRange>)

private val parser = MarkdownParser(GFMFlavourDescriptor())
private val escape = Regex("""\\[!-/:-@\[-`{-~]""")

/** The markup of [text] from [from] up to [to], with offsets into [text]. */
internal fun parseInline(text: String, from: Int = 0, to: Int = text.length): InlineMarkup {
    if (from >= to) return InlineMarkup(emptyList(), emptyList())
    val slice = text.substring(from, to)
    val root = parser.parseInline(MarkdownElementTypes.PARAGRAPH, slice, 0, slice.length)
    val spans = ArrayList<InlineSpan>()
    val markers = ArrayList<IntRange>()
    collect(root, slice, from, spans, markers)
    addEscapes(slice, from, spans, markers)
    return InlineMarkup(spans.sortedBy { it.start }, mergeRanges(markers))
}

private fun collect(node: ASTNode, text: String, shift: Int, spans: MutableList<InlineSpan>, markers: MutableList<IntRange>) {
    val start = node.startOffset + shift
    val end = node.endOffset + shift
    fun hideEnds(width: Int) {
        markers += start until start + width
        markers += end - width until end
    }
    when (node.type) {
        MarkdownElementTypes.STRONG -> {
            spans += InlineSpan(start + 2, end - 2, InlineStyle.Bold)
            hideEnds(2)
        }
        MarkdownElementTypes.EMPH -> {
            spans += InlineSpan(start + 1, end - 1, InlineStyle.Italic)
            hideEnds(1)
        }
        GFMElementTypes.STRIKETHROUGH -> {
            val width = node.children.takeWhile { it.type == GFMTokenTypes.TILDE }.size
            spans += InlineSpan(start + width, end - width, InlineStyle.Strike)
            hideEnds(width)
        }
        MarkdownElementTypes.CODE_SPAN -> {
            val width = node.children.takeWhile { it.type == MarkdownTokenTypes.BACKTICK }.sumOf { it.endOffset - it.startOffset }
            spans += InlineSpan(start + width, end - width, InlineStyle.Code)
            hideEnds(width)
            return
        }
        MarkdownElementTypes.INLINE_LINK -> {
            addLink(node, text, shift, spans, markers)
            return
        }
        MarkdownElementTypes.AUTOLINK -> {
            spans += InlineSpan(start + 1, end - 1, InlineStyle.Link, text.substring(node.startOffset + 1, node.endOffset - 1))
            hideEnds(1)
            return
        }
        GFMTokenTypes.GFM_AUTOLINK -> {
            spans += InlineSpan(start, end, InlineStyle.Link, node.getTextInNode(text).toString())
            return
        }
    }
    node.children.forEach { collect(it, text, shift, spans, markers) }
}

private fun childOf(node: ASTNode, type: IElementType) = node.children.firstOrNull { it.type == type }

/** `[text](url)`: the text stays, and everything else is a marker. */
private fun addLink(node: ASTNode, text: String, shift: Int, spans: MutableList<InlineSpan>, markers: MutableList<IntRange>) {
    val label = childOf(node, MarkdownElementTypes.LINK_TEXT) ?: return
    val url = childOf(node, MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(text)?.toString().orEmpty()
    spans += InlineSpan(label.startOffset + 1 + shift, label.endOffset - 1 + shift, InlineStyle.Link, url)
    markers += label.startOffset + shift until label.startOffset + 1 + shift
    markers += label.endOffset - 1 + shift until node.endOffset + shift
    label.children.forEach { collect(it, text, shift, spans, markers) }
}

/** `\*` shows as `*`, except inside code where the backslash is literal. */
private fun addEscapes(text: String, shift: Int, spans: List<InlineSpan>, markers: MutableList<IntRange>) {
    for (match in escape.findAll(text)) {
        val at = match.range.first + shift
        val inCode = spans.any { it.style == InlineStyle.Code && at >= it.start && at < it.end }
        if (!inCode) markers += at..at
    }
}

internal fun mergeRanges(ranges: List<IntRange>): List<IntRange> {
    val merged = ArrayList<IntRange>()
    for (range in ranges.filter { !it.isEmpty() }.sortedBy { it.first }) {
        val last = merged.lastOrNull()
        if (last != null && range.first <= last.last + 1) {
            merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
        } else {
            merged += range
        }
    }
    return merged
}
