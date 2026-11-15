package dev.chieppa.stretch.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

/** The room above and below a cell's text. */
private val CELL_PADDING_VERTICAL = 5.dp

/** The room a picture without a width of its own is given when sizing columns: about a badge's. */
private val PICTURE_ROOM = 96.dp

/**
 * A markdown table in a [MarkdownView]: each cell as its text reads, with links, code, inline HTML and pictures, and
 * columns sized to the text that shows rather than to its markdown, as [TableView] sizes them.
 */
@Composable
internal fun ReadOnlyTableView(block: MarkdownBlock, context: BlockContext) {
    val c = Stretch.colors
    val styles = rememberRichStyles()
    val table = remember(block.source) { parseTable(block.source) }
    val cells = remember(table, styles) { table.rows.map { row -> row.map { readOnlyText(it, BlockKind.Paragraph, styles) } } }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val body = Stretch.type.body
    val header = body.copy(fontWeight = FontWeight.SemiBold)
    val extents = remember(cells, body, density) {
        with(density) {
            val shown = cells.map { row -> row.map { it.text.text } }
            columnExtents(shown, table.columns, measurer, body, header, (CELL_PADDING * 2).toPx(), MIN_COLUMN_WIDTH.toPx()) { row, column ->
                cells[row].getOrNull(column)?.pictures.orEmpty().sumOf { (it.width?.dp ?: PICTURE_ROOM).toPx().toDouble() }.toFloat()
            }
        }
    }
    val shape = Stretch.shapes.ctlShape
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val widths = columnWidths(extents, constraints.maxWidth.toFloat())
        val tableWidth = with(density) { widths.sum().toDp() }
        Column(
            (if (tableWidth > maxWidth) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
                .width(tableWidth)
                .clip(shape)
                .border(Stretch.stroke, c.lineControl, shape),
        ) {
            cells.forEachIndexed { rowIndex, row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .then(if (rowIndex == 0) Modifier.background(c.surfaceControl) else Modifier)
                        .columnLines(widths, with(density) { Stretch.stroke.toPx() }, c.linePanel),
                ) {
                    row.forEachIndexed { columnIndex, cell ->
                        val style = bodyStyle.merge(
                            TextStyle(textAlign = alignOf(table.aligns.getOrElse(columnIndex) { ColumnAlign.Start }), fontWeight = if (rowIndex == 0) FontWeight.SemiBold else null),
                        )
                        val cellWidth = with(density) { widths.getOrElse(columnIndex) { 0f }.toDp() }
                        Box(Modifier.width(cellWidth).padding(horizontal = CELL_PADDING, vertical = CELL_PADDING_VERTICAL)) {
                            RichTextView(cell, style, Modifier.fillMaxWidth(), context.images, context.onLinkClick)
                        }
                    }
                }
                if (rowIndex < cells.lastIndex) Box(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
            }
        }
    }
}

private fun alignOf(align: ColumnAlign): TextAlign = when (align) {
    ColumnAlign.Start -> TextAlign.Start
    ColumnAlign.Center -> TextAlign.Center
    ColumnAlign.End -> TextAlign.End
}
