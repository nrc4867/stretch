package dev.chieppa.stretch.code

/**
 * [tokens] of a whole [text], with offsets counted in [text], split onto its lines the way [CodeText.of] splits them:
 * one list per line, offsets relative to the start of the line. A token crossing lines is cut at each line's end. Tokens
 * may come in any order and may nest; each line keeps them in the order given, so a later one draws over an earlier one.
 * This turns a parser's spans, such as an app's own syntax tree, into what [CodeViewer] takes.
 */
fun tokensByLine(text: String, tokens: List<CodeToken>): List<List<CodeToken>> {
    val starts = ArrayList<Int>()
    val ends = ArrayList<Int>()
    var lineStart = 0
    var at = 0
    while (at < text.length) {
        val char = text[at]
        if (char == '\n' || char == '\r') {
            starts += lineStart
            ends += at
            at += if (char == '\r' && text.getOrNull(at + 1) == '\n') 2 else 1
            lineStart = at
        } else {
            at++
        }
    }
    starts += lineStart
    ends += text.length
    val lines = List(starts.size) { ArrayList<CodeToken>() }
    for (token in tokens) {
        var line = lineOf(starts, token.start)
        while (line < starts.size && starts[line] < token.end) {
            val start = maxOf(token.start, starts[line]) - starts[line]
            val end = minOf(token.end, ends[line]) - starts[line]
            if (end > start) lines[line] += CodeToken(start, end, token.kind)
            line++
        }
    }
    return lines
}

/** The last line of [starts] beginning at or before [offset]. */
private fun lineOf(starts: List<Int>, offset: Int): Int {
    val found = starts.binarySearch(offset)
    return if (found >= 0) found else (-found - 2).coerceAtLeast(0)
}
