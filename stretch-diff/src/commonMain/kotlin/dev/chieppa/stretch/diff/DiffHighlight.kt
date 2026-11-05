package dev.chieppa.stretch.diff

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import dev.chieppa.stretch.code.CodeColors
import dev.chieppa.stretch.code.CodeLanguages
import dev.chieppa.stretch.code.CodeToken
import dev.chieppa.stretch.code.spanOf
import dev.chieppa.stretch.code.tokenizeLines

private const val TAB_WIDTH = 4

/** Tokens of every line of a set of files, found by which file, which side and which line number. */
class DiffTokens internal constructor(private val byLine: Map<LineKey, List<CodeToken>>) {
    internal data class LineKey(val file: Int, val newSide: Boolean, val number: Int)

    internal fun of(file: Int, line: DiffLine): List<CodeToken> {
        val newSide = line.kind != LineKind.Removed
        val number = (if (newSide) line.newNumber else line.oldNumber) ?: return emptyList()
        return byLine[LineKey(file, newSide, number)].orEmpty()
    }

    companion object {
        val None = DiffTokens(emptyMap())
    }
}

/** Tokenizes each side of [files], one hunk at a time, by the language of each file's path. */
fun tokenizeDiff(files: List<FileDiff>): DiffTokens {
    val tokens = HashMap<DiffTokens.LineKey, List<CodeToken>>()
    files.forEachIndexed { index, file ->
        val language = CodeLanguages.forPath(file.path) ?: return@forEachIndexed
        for (hunk in file.hunks) {
            for (newSide in listOf(false, true)) {
                val lines = hunk.lines.filter { if (newSide) it.kind != LineKind.Removed else it.kind != LineKind.Added }
                val found = tokenizeLines(lines.map { it.text }, language)
                lines.forEachIndexed { at, line ->
                    val number = if (newSide) line.newNumber else line.oldNumber
                    if (number != null) tokens[DiffTokens.LineKey(index, newSide, number)] = found[at]
                }
            }
        }
    }
    return DiffTokens(tokens)
}

/** The text of [line] with tabs expanded, code colored and changed words filled with [changeFill]. */
internal fun lineText(line: DiffLine, tokens: List<CodeToken>, colors: CodeColors, changeFill: Color): AnnotatedString {
    val offsets = tabOffsets(line.text)
    return buildAnnotatedString {
        append(expandTabs(line.text))
        for (token in tokens) {
            addStyle(colors.spanOf(token.kind), offsets[token.start.coerceAtMost(line.text.length)], offsets[token.end.coerceAtMost(line.text.length)])
        }
        for (change in line.changes) {
            addStyle(SpanStyle(background = changeFill), offsets[change.start.coerceAtMost(line.text.length)], offsets[change.end.coerceAtMost(line.text.length)])
        }
    }
}

internal fun expandTabs(text: String): String = if ('\t' in text) text.replace("\t", " ".repeat(TAB_WIDTH)) else text

/** Where each offset of [text] lands once its tabs are expanded, with one entry past the end. */
private fun tabOffsets(text: String): IntArray {
    val offsets = IntArray(text.length + 1)
    var at = 0
    text.forEachIndexed { index, char ->
        offsets[index] = at
        at += if (char == '\t') TAB_WIDTH else 1
    }
    offsets[text.length] = at
    return offsets
}
