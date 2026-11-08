package dev.chieppa.stretch.diff

import dev.chieppa.stretch.code.CodeDecoration
import dev.chieppa.stretch.code.CodeDecorationStyle
import dev.chieppa.stretch.code.CodeRange

/**
 * A run of code an app sets apart in a [DiffViewer]: [range], whose line counts on [side] of the file at [fileIndex],
 * drawn in [style] with [tooltip] shown while the pointer rests on it. Plain data, as [CodeDecoration] is, and drawn and
 * composed the same way. A line both sides share, in one column, shows the decorations of both its numbers.
 */
data class DiffDecoration(
    val fileIndex: Int,
    val side: DiffSide,
    val range: CodeRange,
    val style: CodeDecorationStyle,
    val tooltip: String? = null,
)

/** An app's decorations, found by the line they are on. */
internal class DiffDecorations(decorations: List<DiffDecoration>) {
    private val byLine = decorations.groupBy({ Triple(it.fileIndex, it.side, it.range.line) }) { CodeDecoration(it.range, it.style, it.tooltip) }

    /** The decorations on line [number] of [side] of file [fileIndex], none for a null number. */
    fun on(fileIndex: Int, side: DiffSide, number: Int?): List<CodeDecoration> =
        number?.let { byLine[Triple(fileIndex, side, it)] }.orEmpty()

    companion object {
        val None = DiffDecorations(emptyList())
    }
}
