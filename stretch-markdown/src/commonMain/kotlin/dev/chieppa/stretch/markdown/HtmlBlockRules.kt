package dev.chieppa.stretch.markdown

/** Tag names that start an HTML block ending at a blank line, and that may interrupt a paragraph (CommonMark's kind 6). */
private const val BLOCK_TAG_NAMES = "address|article|aside|base|basefont|blockquote|body|caption|center|col|colgroup|dd|details|" +
    "dialog|dir|div|dl|dt|fieldset|figcaption|figure|footer|form|frame|frameset|h[1-6]|head|header|hr|html|iframe|legend|li|" +
    "link|main|menu|menuitem|nav|noframes|ol|optgroup|option|p|param|search|section|summary|table|tbody|td|tfoot|th|thead|" +
    "title|tr|track|ul"

private const val ATTRIBUTE = """\s+[a-zA-Z_:][a-zA-Z0-9_.:-]*(?:\s*=\s*(?:[^\s"'=<>`]+|'[^']*'|"[^"]*"))?"""

/** HTML blocks that end on the line holding a closing marker, by what opens them, with that marker (kinds 1 to 5). */
private val closedBlocks = listOf(
    Regex("""^ {0,3}<(script|pre|style|textarea)(\s|>|$)""", RegexOption.IGNORE_CASE) to Regex("""</(script|pre|style|textarea)>""", RegexOption.IGNORE_CASE),
    Regex("""^ {0,3}<!--""") to Regex("""-->"""),
    Regex("""^ {0,3}<\?""") to Regex("""\?>"""),
    Regex("""^ {0,3}<![A-Za-z]""") to Regex(""">"""),
    Regex("""^ {0,3}<!\[CDATA\[""") to Regex("""]]>"""),
)

private val blockTagLine = Regex("""^ {0,3}</?($BLOCK_TAG_NAMES)(\s|/?>|$)""", RegexOption.IGNORE_CASE)

/** A line holding one whole opening or closing tag and nothing else (kind 7). */
private val loneTag = Regex("""^ {0,3}(?:<[a-zA-Z][a-zA-Z0-9-]*(?:$ATTRIBUTE)*\s*/?>|</[a-zA-Z][a-zA-Z0-9-]*\s*>)\s*$""")

/** Whether [line] starts an HTML block that may cut a paragraph short: every kind but a lone unknown tag. */
internal fun interruptsWithHtml(line: String): Boolean = closedBlocks.any { it.first.containsMatchIn(line) } || blockTagLine.containsMatchIn(line)

/**
 * The line after the end of the HTML block that [lines] start at [start], read as CommonMark and GitHub read one: a
 * comment, `<pre>` and their like end on the line that closes them, and other blocks at a blank line. Null when the
 * line starts no HTML block, as an inline tag with text after it starts a paragraph.
 */
internal fun htmlBlockEnd(lines: List<String>, start: Int): Int? {
    val line = lines[start]
    closedBlocks.firstOrNull { it.first.containsMatchIn(line) }?.let { (_, close) ->
        for (at in start until lines.size) if (close.containsMatchIn(lines[at])) return at + 1
        var end = lines.size
        while (end > start + 1 && lines[end - 1].isBlank()) end--
        return end
    }
    if (!blockTagLine.containsMatchIn(line) && !loneTag.matches(line)) return null
    var at = start + 1
    while (at < lines.size && lines[at].isNotBlank()) at++
    return at
}
