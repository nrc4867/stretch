package dev.chieppa.stretch.code

/** How many columns a tab advances, in every code view. */
const val CODE_TAB_WIDTH = 4

/**
 * A text split into lines, the model code views draw from. Line numbers count from 1, as people read them. Any of
 * `\n`, `\r\n` and `\r` ends a line; a text ending in a line break has an empty last line, as editors show.
 */
class CodeText(val lines: List<String>) {
    val lineCount: Int get() = lines.size

    /** The text of line [number], counting from 1. */
    fun line(number: Int): String = lines[number - 1]

    /** The lines joined with `\n`. */
    val text: String get() = lines.joinToString("\n")

    companion object {
        fun of(text: String): CodeText = CodeText(text.split("\r\n", "\n", "\r"))
    }
}

/** A place in a [CodeText]: [line] counts from 1, and the characters from [start] up to [end] are on it. */
data class CodeRange(val line: Int, val start: Int, val end: Int)

/** Every place [query] occurs in [lines], top to bottom, ignoring case unless [matchCase]. None for an empty query. */
fun findInLines(lines: List<String>, query: String, matchCase: Boolean = false): List<CodeRange> {
    if (query.isEmpty()) return emptyList()
    return buildList {
        lines.forEachIndexed { index, line ->
            var at = line.indexOf(query, 0, ignoreCase = !matchCase)
            while (at >= 0) {
                add(CodeRange(index + 1, at, at + query.length))
                at = line.indexOf(query, at + query.length, ignoreCase = !matchCase)
            }
        }
    }
}

/** [text] with each tab replaced by [CODE_TAB_WIDTH] spaces. */
fun expandTabs(text: String): String = if ('\t' in text) text.replace("\t", " ".repeat(CODE_TAB_WIDTH)) else text

/** Where each offset of [text] lands once its tabs are expanded, with one entry past the end. */
fun tabOffsets(text: String): IntArray {
    val offsets = IntArray(text.length + 1)
    var at = 0
    text.forEachIndexed { index, char ->
        offsets[index] = at
        at += if (char == '\t') CODE_TAB_WIDTH else 1
    }
    offsets[text.length] = at
    return offsets
}
