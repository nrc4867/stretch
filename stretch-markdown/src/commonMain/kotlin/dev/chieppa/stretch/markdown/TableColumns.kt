package dev.chieppa.stretch.markdown

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/** The narrowest a column gets, so a one-letter column still has room for its padding and a word or two. */
internal val MIN_COLUMN_WIDTH = 48.dp

/** How wide one column of a table can be: its longest word and its longest cell on one line, padding included. */
internal data class ColumnExtent(val least: Float, val most: Float)

/**
 * The widths, in pixels, of a table's columns from what their cells need ([columnExtents]), laid out the way a web
 * page lays out a table. When every cell fits on one line in [available], each column takes that and the rest is
 * shared out by the same proportion. Otherwise each column keeps its longest word and the room left goes to the
 * columns with the most text, which wrap. Only when the longest words alone are wider than [available] is the table
 * wider than that.
 */
internal fun columnWidths(extents: List<ColumnExtent>, available: Float): List<Float> {
    if (extents.isEmpty()) return emptyList()
    val most = extents.sumOf { it.most.toDouble() }.toFloat()
    if (most <= available) return extents.map { it.most + (available - most) * it.most / most }
    val least = extents.sumOf { it.least.toDouble() }.toFloat()
    if (least >= available) return extents.map { it.least }
    val spare = available - least
    val wants = extents.sumOf { (it.most - it.least).toDouble() }.toFloat()
    return extents.map { it.least + spare * (it.most - it.least) / wants }
}

/** Measures what each column of [table] needs, the header row in [header] and the rest in [body], plus [padding]. */
internal fun columnExtents(table: MarkdownTable, measurer: TextMeasurer, body: TextStyle, header: TextStyle, padding: Float, floor: Float): List<ColumnExtent> =
    (0 until table.columns).map { column ->
        var least = floor
        var most = floor
        table.rows.forEachIndexed { row, cells ->
            val text = cells.getOrElse(column) { "" }
            val style = if (row == 0) header else body
            val line = measurer.measure(text, style, softWrap = false).size.width + padding
            val word = text.split(' ').maxOfOrNull { measurer.measure(it, style, softWrap = false).size.width.toFloat() } ?: 0f
            most = maxOf(most, line)
            least = maxOf(least, word + padding)
        }
        ColumnExtent(least, maxOf(least, most))
    }

/** Draws a [width] line in [color] at the right edge of each column but the last, down the row's whole height. */
internal fun Modifier.columnLines(widths: List<Float>, width: Float, color: Color): Modifier = drawBehind {
    var x = 0f
    widths.dropLast(1).forEach { column ->
        x += column
        drawRect(color, Offset(x - width / 2, 0f), Size(width, size.height))
    }
}
