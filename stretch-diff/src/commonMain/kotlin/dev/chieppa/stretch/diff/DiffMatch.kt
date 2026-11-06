package dev.chieppa.stretch.diff

import dev.chieppa.stretch.code.CodeHighlight
import dev.chieppa.stretch.code.CodeLook
import dev.chieppa.stretch.code.findInLines
import dev.chieppa.stretch.code.matchHighlight

/**
 * A find match in a [DiffViewer]: characters [start] until [end] of line [line], counting from 1, on [side] of the file
 * at [fileIndex] in the viewer's list. On a line both sides share, either side's match shows on it.
 */
data class DiffMatch(val fileIndex: Int, val side: DiffSide, val line: Int, val start: Int, val end: Int)

/**
 * Every place [query] occurs in the lines of [files], in order, ignoring case unless [matchCase]. A removed line's
 * matches are on the old side and every other line's on the new side, so a line both sides share counts once.
 */
fun findInDiff(files: List<FileDiff>, query: String, matchCase: Boolean = false): List<DiffMatch> {
    if (query.isEmpty()) return emptyList()
    return buildList {
        files.forEachIndexed { fileIndex, file ->
            for (line in file.hunks.flatMap { it.lines }) {
                val side = if (line.kind == LineKind.Removed) DiffSide.Old else DiffSide.New
                val number = line.numberOn(side) ?: continue
                findInLines(listOf(line.text), query, matchCase).forEach { add(DiffMatch(fileIndex, side, number, it.start, it.end)) }
            }
        }
    }
}

/** An app's find matches, found by line, with the [current] one set apart. */
internal class DiffFinds(matches: List<DiffMatch>, private val current: DiffMatch?) {
    private val byLine = matches.groupBy { Triple(it.fileIndex, it.side, it.line) }

    /** How the matches on [line] of file [fileIndex] draw, as one column shows it, or one [side] when given. */
    fun highlights(fileIndex: Int, line: DiffLine, look: CodeLook, side: DiffSide? = null): List<CodeHighlight> {
        val old = if (side != DiffSide.New) line.oldNumber?.let { byLine[Triple(fileIndex, DiffSide.Old, it)] }.orEmpty() else emptyList()
        val new = if (side != DiffSide.Old) line.newNumber?.let { byLine[Triple(fileIndex, DiffSide.New, it)] }.orEmpty() else emptyList()
        if (old.isEmpty() && new.isEmpty()) return emptyList()
        val here = old + new
        // A line both sides share can hold the same match twice, once per side; it draws once.
        val currentRange = current?.takeIf { it in here }?.let { it.start to it.end }
        return here.map { it.start to it.end }.distinct().map { (start, end) -> look.matchHighlight(start, end, (start to end) == currentRange) }
    }

    companion object {
        val None = DiffFinds(emptyList(), null)
    }
}
