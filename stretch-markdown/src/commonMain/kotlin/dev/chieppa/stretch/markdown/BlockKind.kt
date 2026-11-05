package dev.chieppa.stretch.markdown

enum class ListStyle { Bullet, Ordered, Task }

/** The five kinds of GitHub alert: `> [!NOTE]` and its siblings. */
enum class CalloutKind(val tag: String) { Note("NOTE"), Tip("TIP"), Important("IMPORTANT"), Warning("WARNING"), Caution("CAUTION") }

/** What a block's source makes it. */
sealed interface BlockKind {
    data object Paragraph : BlockKind

    /** `# Title`, or a [setext] one underlined with `===` or `---`. */
    data class Heading(val level: Int, val setext: Boolean = false) : BlockKind

    /** One item, nested [indent] spaces in. [markerEnd] is where its text starts in the source. */
    data class ListItem(val style: ListStyle, val indent: Int, val markerEnd: Int, val number: Int = 0, val checked: Boolean = false) : BlockKind

    data object Quote : BlockKind

    data class Callout(val kind: CalloutKind) : BlockKind

    /** A fenced block. [closed] says whether a closing fence is there yet. */
    data class Code(val language: String, val fence: String, val closed: Boolean) : BlockKind

    data object Table : BlockKind

    /** A line holding one image. */
    data class Image(val alt: String, val url: String) : BlockKind

    data object Divider : BlockKind

    /** Source kept as it is and shown as text: HTML, front matter and indented code. */
    data object Raw : BlockKind
}

internal val headingLine = Regex("""^ {0,3}(#{1,6})([ \t]+|$)""")
internal val dividerLine = Regex("""^ {0,3}([-*_])[ \t]*(?:\1[ \t]*){2,}$""")
internal val fenceLine = Regex("""^ {0,3}(`{3,}|~{3,})[ \t]*([^`]*)$""")
internal val listLine = Regex("""^([ \t]*)([-*+]|\d{1,9}[.)])([ \t]+|$)(\[[ xX]\](?:[ \t]+|$))?""")
internal val quoteLine = Regex("""^ {0,3}> ?""")
internal val calloutLine = Regex("""^ {0,3}> ?\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\][ \t]*$""", RegexOption.IGNORE_CASE)
internal val imageLine = Regex("""^!\[([^\]]*)\]\(([^)\s]*)(?:\s+"[^"]*")?\)[ \t]*$""")
internal val tableDelimiterLine = Regex("""^ {0,3}\|?[ \t]*:?-+:?[ \t]*(\|[ \t]*:?-+:?[ \t]*)*\|?[ \t]*$""")
internal val setextLine = Regex("""^ {0,3}(=+|-+)[ \t]*$""")
internal val htmlLine = Regex("""^ {0,3}<(!--|[a-zA-Z/!?])""")
internal val indentedCodeLine = Regex("""^( {4}|\t)""")

internal fun isTableStart(first: String, second: String?): Boolean =
    '|' in first && second != null && '|' in second && tableDelimiterLine.matches(second)

/** What [source], one block's text, is. */
fun classifyBlock(source: String): BlockKind {
    val lines = source.split('\n')
    val first = lines[0]
    fenceLine.find(first)?.let { match ->
        val fence = match.groupValues[1]
        val closed = lines.size > 1 && lines.last().trim().let { it.length >= fence.length && it.all { char -> char == fence[0] } }
        return BlockKind.Code(match.groupValues[2].trim().substringBefore(' '), fence, closed)
    }
    if (lines.size >= 3 && first.trimEnd() == "---" && lines.last().trimEnd() == "---") return BlockKind.Raw
    headingLine.find(first)?.let { return BlockKind.Heading(it.groupValues[1].length) }
    if (dividerLine.matches(first) && lines.size == 1) return BlockKind.Divider
    if (lines.size == 2 && first.isNotBlank() && setextLine.matches(lines[1]) && !listLine.containsMatchIn(first) && !quoteLine.containsMatchIn(first)) {
        return BlockKind.Heading(if (lines[1].trim().startsWith("=")) 1 else 2, setext = true)
    }
    if (isTableStart(first, lines.getOrNull(1))) return BlockKind.Table
    calloutLine.find(first)?.let { match ->
        return BlockKind.Callout(CalloutKind.entries.first { it.tag == match.groupValues[1].uppercase() })
    }
    if (quoteLine.containsMatchIn(first)) return BlockKind.Quote
    listLine.find(first)?.let { match -> return listItemOf(match) }
    imageLine.find(first)?.let { match -> if (lines.size == 1) return BlockKind.Image(match.groupValues[1], match.groupValues[2]) }
    if (htmlLine.containsMatchIn(first) || indentedCodeLine.containsMatchIn(first)) return BlockKind.Raw
    return BlockKind.Paragraph
}

private fun listItemOf(match: MatchResult): BlockKind.ListItem {
    val (indent, marker, _, box) = match.destructured
    val ordered = marker.first().isDigit()
    return BlockKind.ListItem(
        style = when {
            box.isNotEmpty() -> ListStyle.Task
            ordered -> ListStyle.Ordered
            else -> ListStyle.Bullet
        },
        indent = indent.length,
        markerEnd = match.range.last + 1,
        number = if (ordered) marker.dropLast(1).toInt() else 0,
        checked = box.getOrNull(1)?.lowercaseChar() == 'x',
    )
}

/**
 * One block of a document. [source] is its exact markdown, without what comes after it; [separator] is the whitespace
 * between it and the next block. Joining them gives the file back byte for byte, so editing one block never reformats
 * the rest. [id] follows a block through edits.
 */
class MarkdownBlock(val id: Long, val source: String, val separator: String = DEFAULT_SEPARATOR) {
    val kind: BlockKind = classifyBlock(source)

    fun copy(source: String = this.source, separator: String = this.separator) = MarkdownBlock(id, source, separator)

    override fun equals(other: Any?) = other is MarkdownBlock && other.id == id && other.source == source && other.separator == separator

    override fun hashCode() = 31 * (31 * id.hashCode() + source.hashCode()) + separator.hashCode()

    override fun toString() = "MarkdownBlock($id, ${source.take(40)})"

    companion object {
        const val DEFAULT_SEPARATOR = "\n\n"
        const val LIST_SEPARATOR = "\n"
    }
}
