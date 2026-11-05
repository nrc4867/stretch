package dev.chieppa.stretch.markdown

private const val FRONT_MATTER_FENCE = "---"

/** Hands out block ids, unique within a process so blocks from a paste can never clash with those they join. */
internal object BlockIds {
    private var next = 1L

    fun next(): Long = next++
}

/** Whitespace-only lines before the first block, and the blocks. Joining all of them gives back the text they came from. */
internal class ParsedBlocks(val leading: String, val blocks: List<MarkdownBlock>)

/** Splits [text], with `\n` line endings, into blocks. Each list item is a block of its own, nested ones too. */
internal fun parseBlocks(text: String): ParsedBlocks {
    val lines = text.split('\n')
    var at = 0
    while (at < lines.size - 1 && lines[at].isBlank()) at++
    val leading = lines.subList(0, at).joinToString("") { it + "\n" }
    val blocks = ArrayList<MarkdownBlock>()
    var first = true
    while (at < lines.size) {
        if (lines[at].isBlank() && at == lines.size - 1) {
            if (blocks.isEmpty()) blocks += MarkdownBlock(BlockIds.next(), lines[at], "")
            break
        }
        val end = blockEnd(lines, at, first)
        first = false
        var next = end
        while (next < lines.size && lines[next].isBlank()) next++
        val separator = if (next >= lines.size) {
            lines.subList(end, lines.size).joinToString("") { "\n$it" }
        } else {
            "\n" + lines.subList(end, next).joinToString("") { it + "\n" }
        }
        blocks += MarkdownBlock(BlockIds.next(), lines.subList(at, end).joinToString("\n"), separator)
        at = next
    }
    return ParsedBlocks(leading, blocks)
}

/** The line after the end of the block starting at [start]. */
private fun blockEnd(lines: List<String>, start: Int, first: Boolean): Int {
    val line = lines[start]
    if (first && line.trimEnd() == FRONT_MATTER_FENCE) {
        val close = (start + 1 until lines.size).firstOrNull { lines[it].trimEnd() == FRONT_MATTER_FENCE }
        if (close != null && close > start + 1 && ':' in lines[start + 1]) return close + 1
    }
    fenceLine.find(line)?.let { return fenceEnd(lines, start, it.groupValues[1]) }
    if (headingLine.containsMatchIn(line) || dividerLine.matches(line)) return start + 1
    if (isTableStart(line, lines.getOrNull(start + 1))) return runEnd(lines, start + 2) { '|' in it && it.isNotBlank() }
    if (quoteLine.containsMatchIn(line)) return runEnd(lines, start + 1) { quoteLine.containsMatchIn(it) }
    listLine.find(line)?.let { return listItemEnd(lines, start, it.groupValues[1].length + it.groupValues[2].length + 1) }
    if (imageLine.matches(line) && (start + 1 >= lines.size || lines[start + 1].isBlank())) return start + 1
    if (indentedCodeLine.containsMatchIn(line)) return indentedEnd(lines, start)
    if (htmlLine.containsMatchIn(line)) return runEnd(lines, start + 1) { it.isNotBlank() }
    return paragraphEnd(lines, start)
}

private fun runEnd(lines: List<String>, from: Int, inside: (String) -> Boolean): Int {
    var at = from
    while (at < lines.size && inside(lines[at])) at++
    return at
}

private fun fenceEnd(lines: List<String>, start: Int, fence: String): Int {
    for (at in start + 1 until lines.size) {
        val trimmed = lines[at].trim()
        if (trimmed.length >= fence.length && trimmed.all { it == fence[0] }) return at + 1
    }
    // An unclosed fence runs to the end of the file; lines blank at the very end stay out of it.
    var end = lines.size
    while (end > start + 1 && lines[end - 1].isBlank()) end--
    return end
}

/** An item runs on over lines indented under it, blank ones between included, until the next item or a shallower line. */
private fun listItemEnd(lines: List<String>, start: Int, contentIndent: Int): Int {
    var at = start + 1
    var end = at
    while (at < lines.size) {
        val line = lines[at]
        if (line.isBlank()) {
            at++
            continue
        }
        val indent = line.length - line.trimStart().length
        val lazy = at == end && !startsBlock(line, lines.getOrNull(at + 1))
        if (listLine.containsMatchIn(line) || (indent < contentIndent && !lazy)) break
        at++
        end = at
    }
    return end
}

private fun indentedEnd(lines: List<String>, start: Int): Int {
    var at = start + 1
    var end = at
    while (at < lines.size) {
        if (lines[at].isBlank()) {
            at++
            continue
        }
        if (!indentedCodeLine.containsMatchIn(lines[at])) break
        at++
        end = at
    }
    return end
}

private fun startsBlock(line: String, next: String?): Boolean =
    line.isBlank() || fenceLine.containsMatchIn(line) || headingLine.containsMatchIn(line) || dividerLine.matches(line) ||
        quoteLine.containsMatchIn(line) || listLine.containsMatchIn(line) || isTableStart(line, next) || htmlLine.containsMatchIn(line)

private fun paragraphEnd(lines: List<String>, start: Int): Int {
    var at = start + 1
    while (at < lines.size) {
        val line = lines[at]
        if (setextLine.matches(line) && at == start + 1) return at + 1
        if (startsBlock(line, lines.getOrNull(at + 1))) return at
        at++
    }
    return at
}
