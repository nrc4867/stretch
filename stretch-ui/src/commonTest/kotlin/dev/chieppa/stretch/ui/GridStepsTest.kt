package dev.chieppa.stretch.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class GridStepsTest {
    // Seven cells, three to a row:
    // 0 1 2
    // 3 4 5
    // 6
    private fun step(from: Int?, move: GridMove, disabled: Set<Int> = emptySet()) =
        gridStep(from, 7, 3, move) { it !in disabled }

    @Test
    fun leftAndRightStepThroughTheCellsAndStopAtTheEnds() {
        assertEquals(GridStep.To(3), step(2, GridMove.Right))
        assertEquals(GridStep.To(2), step(3, GridMove.Left))
        assertEquals(GridStep.Stay, step(0, GridMove.Left))
        assertEquals(GridStep.Stay, step(6, GridMove.Right))
    }

    @Test
    fun upAndDownKeepTheColumn() {
        assertEquals(GridStep.To(4), step(1, GridMove.Down))
        assertEquals(GridStep.To(1), step(4, GridMove.Up))
    }

    @Test
    fun aShortLastRowTakesTheCursorAtItsNearestCell() {
        assertEquals(GridStep.To(6), step(5, GridMove.Down))
    }

    @Test
    fun upFromTheFirstRowAndDownFromTheLastLeaveTheGrid() {
        assertEquals(GridStep.LeaveTop, step(2, GridMove.Up))
        assertEquals(GridStep.LeaveBottom, step(6, GridMove.Down))
    }

    @Test
    fun disabledCellsAreSkipped() {
        assertEquals(GridStep.To(4), step(2, GridMove.Right, disabled = setOf(3)))
        assertEquals(GridStep.To(5), step(2, GridMove.Down, disabled = setOf()))
        assertEquals(GridStep.To(3), step(0, GridMove.Down, disabled = setOf()))
        // Row two's cell under the cursor is off: the nearest one in that row takes it.
        assertEquals(GridStep.To(3), step(1, GridMove.Down, disabled = setOf(4)))
        // A whole row off: the next row with a cell that can take it.
        assertEquals(GridStep.To(6), step(1, GridMove.Down, disabled = setOf(3, 4, 5)))
        assertEquals(GridStep.LeaveBottom, step(4, GridMove.Down, disabled = setOf(6)))
    }

    @Test
    fun withNoCursorTheFirstOrLastEnabledCellTakesIt() {
        assertEquals(GridStep.To(1), step(null, GridMove.Down, disabled = setOf(0)))
        assertEquals(GridStep.To(5), step(null, GridMove.Up, disabled = setOf(6)))
        assertEquals(GridStep.LeaveBottom, gridStep(null, 0, 3, GridMove.Down) { true })
    }
}
