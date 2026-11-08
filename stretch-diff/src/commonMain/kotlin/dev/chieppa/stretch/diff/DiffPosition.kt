package dev.chieppa.stretch.diff

import androidx.compose.ui.geometry.Rect
import dev.chieppa.stretch.code.CodeRange

/**
 * A place in a [DiffViewer]'s code: between characters of line [line], counting from 1, on [side] of the file at
 * [fileIndex] in the viewer's list, [column] being the offset of the character after it within the line before tabs
 * expand. Side by side, the left half is [DiffSide.Old] and the right [DiffSide.New]; in one column a removed line is
 * on the old side and every other line on the new, as [DiffLineRef] counts.
 */
data class DiffPosition(val fileIndex: Int, val side: DiffSide, val line: Int, val column: Int)

/**
 * A click through to a symbol in a [DiffViewer]: [position] is where it was clicked, or the caret for a key, and [range]
 * the symbol the app named there, on the same side of the same file. [bounds] is where [range] shows, in pixels from
 * the viewer's top left as it is laid out with its modifier, to open a popup at, such as a `Popover` or `Menu` with
 * `anchor = bounds` composed in a `Box` the viewer fills. It is null only when the line could not be laid out in time,
 * which a key press far off screen can meet.
 */
data class DiffSymbolClick(val position: DiffPosition, val range: CodeRange, val bounds: Rect?)

/** The lines of one side of one file, which number on their own. */
internal data class DiffFileSide(val fileIndex: Int, val side: DiffSide)
