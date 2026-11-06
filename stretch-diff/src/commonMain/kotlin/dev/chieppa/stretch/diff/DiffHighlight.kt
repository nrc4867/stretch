package dev.chieppa.stretch.diff

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import dev.chieppa.stretch.code.CodeColors
import dev.chieppa.stretch.code.CodeLanguages
import dev.chieppa.stretch.code.CodeToken
import dev.chieppa.stretch.code.CodeHighlight
import dev.chieppa.stretch.code.codeLineText
import dev.chieppa.stretch.code.tokenizeLines
import dev.chieppa.stretch.code.tokensByLine

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

/**
 * Tokens of a whole file from outside, such as from a parser the app runs: [old] for the file as it was and [new] for
 * the file as it is, each one list per line from line 1 with offsets within the line ([tokensByLine] splits a whole
 * text's spans). A [DiffViewer] colors each line from the side it shows; a null side is tokenized from the diff itself.
 */
class FileTokens(val old: List<List<CodeToken>>? = null, val new: List<List<CodeToken>>? = null)

/** The tokens a line draws with: the app's [FileTokens] for its side when given, else [builtIn]'s. */
internal class LineTokens(private val builtIn: DiffTokens, private val supplied: List<FileTokens?>) {
    fun of(file: Int, line: DiffLine): List<CodeToken> {
        val newSide = line.kind != LineKind.Removed
        val number = if (newSide) line.newNumber else line.oldNumber
        val whole = supplied.getOrNull(file)?.let { if (newSide) it.new else it.old }
        if (whole != null && number != null && number <= whole.size) return whole[number - 1]
        return builtIn.of(file, line)
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
