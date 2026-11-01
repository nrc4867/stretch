package dev.chieppa.stretch.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How many columns of at least [minCell] fit in [width] with [gap] between them; never fewer than one. */
fun gridColumns(width: Float, minCell: Float, gap: Float): Int =
    ((width + gap) / (minCell + gap)).toInt().coerceAtLeast(1)

/**
 * A grid of equal-width cells. With [columns] the count is fixed; otherwise as many cells of at least [minCell]
 * as fit, so the grid reflows with its container. Each row is as tall as its tallest cell.
 */
@Composable
fun Grid(
    modifier: Modifier = Modifier,
    minCell: Dp = 160.dp,
    columns: Int? = null,
    gap: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    Layout(content, modifier) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else (minCell.roundToPx() + gapPx) * (columns ?: 3) - gapPx
        val count = columns ?: gridColumns(width.toFloat(), minCell.toPx(), gapPx.toFloat())
        val cell = ((width - gapPx * (count - 1)) / count).coerceAtLeast(0)
        val placeables = measurables.map { it.measure(Constraints(minWidth = cell, maxWidth = cell)) }
        val rows = placeables.chunked(count)
        val heights = rows.map { row -> row.maxOf { it.height } }
        val height = heights.sum() + gapPx * (rows.size - 1).coerceAtLeast(0)
        layout(width, height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var y = 0
            rows.forEachIndexed { r, row ->
                row.forEachIndexed { i, p -> p.place(i * (cell + gapPx), y) }
                y += heights[r] + gapPx
            }
        }
    }
}
