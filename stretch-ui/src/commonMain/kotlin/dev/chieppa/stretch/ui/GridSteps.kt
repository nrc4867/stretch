package dev.chieppa.stretch.ui

/** An arrow key's move in a grid, already turned for the layout direction. */
internal enum class GridMove { Left, Right, Up, Down }

/** Where an arrow key takes a grid's cursor. */
internal sealed interface GridStep {
    /** To the cell at [index]. */
    data class To(val index: Int) : GridStep

    /** Up from the first row with a cell that can take the cursor. */
    data object LeaveTop : GridStep

    /** Down from the last row with a cell that can take the cursor. */
    data object LeaveBottom : GridStep

    /** Nowhere: the cursor is already at the end it moved toward. */
    data object Stay : GridStep
}

/**
 * Where [move] takes the cursor at [from] in a grid of [count] cells laid out [columns] to a row, skipping cells that
 * are not [enabled]. Left and Right step through the cells in order and stop at either end. Up and Down go to the next
 * row that has an enabled cell, to the one nearest the cursor's column, so a short last row still takes the cursor;
 * past the first or last such row they leave the grid. With no cursor, Down and Right go to the first enabled cell and
 * Up and Left to the last.
 */
internal fun gridStep(from: Int?, count: Int, columns: Int, move: GridMove, enabled: (Int) -> Boolean): GridStep {
    val cols = columns.coerceAtLeast(1)
    val all = 0 until count
    if (from == null || from !in all) {
        val index = when (move) {
            GridMove.Down, GridMove.Right -> all.firstOrNull(enabled)
            GridMove.Up, GridMove.Left -> all.reversed().firstOrNull(enabled)
        }
        return when {
            index != null -> GridStep.To(index)
            move == GridMove.Up -> GridStep.LeaveTop
            move == GridMove.Down -> GridStep.LeaveBottom
            else -> GridStep.Stay
        }
    }
    return when (move) {
        GridMove.Left -> (from - 1 downTo 0).firstOrNull(enabled)?.let { GridStep.To(it) } ?: GridStep.Stay
        GridMove.Right -> (from + 1 until count).firstOrNull(enabled)?.let { GridStep.To(it) } ?: GridStep.Stay
        GridMove.Up -> rowStep(from, count, cols, -1, enabled) ?: GridStep.LeaveTop
        GridMove.Down -> rowStep(from, count, cols, 1, enabled) ?: GridStep.LeaveBottom
    }
}

/** The enabled cell nearest [from]'s column in the closest row [direction] of it that has one. */
private fun rowStep(from: Int, count: Int, cols: Int, direction: Int, enabled: (Int) -> Boolean): GridStep? {
    val column = from % cols
    val lastRow = (count - 1) / cols
    var row = from / cols + direction
    while (row in 0..lastRow) {
        val first = row * cols
        val last = minOf(first + cols, count) - 1
        val nearest = (first..last).filter(enabled).minByOrNull { kotlin.math.abs(it - first - column) * 2 + (if (it - first > column) 1 else 0) }
        if (nearest != null) return GridStep.To(nearest)
        row += direction
    }
    return null
}
