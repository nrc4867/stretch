package dev.chieppa.stretch.diff

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import dev.chieppa.stretch.code.CodeColors
import dev.chieppa.stretch.code.CodeLanguages
import dev.chieppa.stretch.code.CodeToken
import dev.chieppa.stretch.code.CodeHighlight
import dev.chieppa.stretch.code.codeLineText
import dev.chieppa.stretch.code.tokenizeLines

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
internal fun lineText(line: DiffLine, tokens: List<CodeToken>, colors: CodeColors, changeFill: Color): AnnotatedString =
    codeLineText(line.text, tokens, colors, line.changes.map { CodeHighlight(it.start, it.end, changeFill) })
