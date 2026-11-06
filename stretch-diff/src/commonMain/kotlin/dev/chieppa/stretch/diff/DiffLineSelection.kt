package dev.chieppa.stretch.diff

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/** Which version of a file a line number counts in: before the change or after it. */
enum class DiffSide { Old, New }

/** Lines [start] to [end], inclusive and counting from 1, on one [side] of the file at [fileIndex] in the viewer's list. */
data class DiffLineRange(val fileIndex: Int, val file: FileDiff, val side: DiffSide, val start: Int, val end: Int) {
    operator fun contains(number: Int): Boolean = number in start..end
}

/** The row's line number on [side], or null when the row has no line there. */
fun DiffRow.numberOn(side: DiffSide): Int? = when (this) {
    is DiffRow.Unified -> if (side == DiffSide.Old) line.oldNumber else line.newNumber
    is DiffRow.Split -> if (side == DiffSide.Old) left?.oldNumber else right?.newNumber
    else -> null
}

/** The file a row belongs to, or null for a row of no one file. */
val DiffRow.fileIndex: Int?
    get() = when (this) {
        is DiffRow.FileHeader -> fileIndex
        is DiffRow.HunkHeader -> fileIndex
        is DiffRow.Unified -> fileIndex
        is DiffRow.Split -> fileIndex
        is DiffRow.Fold -> id.fileIndex
        is DiffRow.Binary -> null
    }

/** The first row at or after [line] on [side] of file [fileIndex], or the last of that file's rows before it. */
fun rowOfLine(rows: List<DiffRow>, fileIndex: Int, side: DiffSide, line: Int): Int? {
    var before: Int? = null
    rows.forEachIndexed { index, row ->
        if (row.fileIndex != fileIndex) return@forEachIndexed
        val number = row.numberOn(side) ?: return@forEachIndexed
        if (number >= line) return index
        before = index
    }
    return before
}

/** The lines from [anchor] to [to] on [side] of file [fileIndex], in order. */
internal fun lineRange(files: List<FileDiff>, fileIndex: Int, side: DiffSide, anchor: Int, to: Int) =
    DiffLineRange(fileIndex, files[fileIndex], side, minOf(anchor, to), maxOf(anchor, to))

/**
 * Pointer input for one line number. A press selects the line, dragging over other numbers of the same file and side
 * extends the selection, and Shift extends it from where the last one started; letting go reports the range to
 * [onRange]. A press that does not move and is not Shift calls [onClick]. Without [onRange] the number only clicks.
 */
internal fun Modifier.lineNumberInput(
    state: DiffViewerState,
    files: List<FileDiff>,
    fileIndex: Int,
    side: DiffSide,
    number: Int,
    clickLabel: String?,
    onClick: (() -> Unit)?,
    onRange: ((DiffLineRange) -> Unit)?,
): Modifier = if (onClick == null && onRange == null) this else composed {
    val holder = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val click by rememberUpdatedState(onClick)
    val range by rememberUpdatedState(onRange)
    val currentFiles by rememberUpdatedState(files)
    this
        .onGloballyPositioned { holder[0] = it }
        .semantics {
            role = Role.Button
            onClick(clickLabel) {
                click?.invoke()
                range?.let { report -> lineRange(currentFiles, fileIndex, side, number, number).also { state.lineSelection = it }.let(report) }
                true
            }
        }
        .pointerInput(fileIndex, side, number) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val shift = currentEvent.keyboardModifiers.isShiftPressed
                val report = range
                if (report == null) {
                    if (waitForUpOrCancellation() != null) click?.invoke()
                    return@awaitEachGesture
                }
                down.consume()
                val previous = state.lineSelection
                val anchor = if (shift && previous != null && previous.fileIndex == fileIndex && previous.side == side) state.selectionAnchor else number
                state.selectionAnchor = anchor
                state.lineSelection = lineRange(currentFiles, fileIndex, side, anchor, number)
                var moved = false
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    if (change.positionChange() != Offset.Zero) {
                        moved = true
                        change.consume()
                        val cell = holder[0] ?: continue
                        val over = state.numberAt(cell.localToWindow(change.position), fileIndex, side) ?: continue
                        state.lineSelection = lineRange(currentFiles, fileIndex, side, anchor, over)
                    }
                }
                if (!moved && !shift) click?.invoke()
                state.lineSelection?.let(report)
            }
        }
}
