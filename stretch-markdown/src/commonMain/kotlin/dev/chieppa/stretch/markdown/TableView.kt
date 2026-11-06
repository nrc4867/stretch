package dev.chieppa.stretch.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.chord
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.holdsTyping

private data class CellAt(val row: Int, val column: Int)

/**
 * A table as a grid of fields. Columns take the width their cells need, as on a web page ([columnWidths]); long cells
 * wrap and their row grows to fit, and only a table whose longest words cannot fit side by side scrolls sideways. Tab
 * moves on a cell, adding a row at the end; Enter moves down.
 */
@Composable
internal fun TableView(block: MarkdownBlock, context: BlockContext) {
    val c = Stretch.colors
    val state = context.state
    val table = remember(block.source) { parseTable(block.source) }
    var active by remember(block.id) { mutableStateOf<CellAt?>(null) }
    var wanted by remember(block.id) { mutableStateOf<CellAt?>(null) }

    LaunchedEffect(state.focusTicket) {
        if (state.focusedBlock == block.id && active == null && !context.readOnly && state.selectedBlocks == null) wanted = CellAt(0, 0)
    }
    val shape = Stretch.shapes.ctlShape
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val body = Stretch.type.body
    val extents = remember(table, body, density) {
        with(density) {
            columnExtents(table, measurer, body, body.copy(fontWeight = FontWeight.SemiBold), (CELL_PADDING * 2).toPx(), MIN_COLUMN_WIDTH.toPx())
        }
    }
    Column(Modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val widths = columnWidths(extents, constraints.maxWidth.toFloat())
            val tableWidth = with(density) { widths.sum().toDp() }
            Column(
                (if (tableWidth > maxWidth) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
                    .width(tableWidth)
                    .clip(shape)
                    .border(Stretch.stroke, c.lineControl, shape),
            ) {
                table.rows.forEachIndexed { rowIndex, row ->
                    key(rowIndex) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .then(if (rowIndex == 0) Modifier.background(c.surfaceControl) else Modifier)
                                .columnLines(widths, with(density) { Stretch.stroke.toPx() }, c.linePanel),
                        ) {
                            row.forEachIndexed { columnIndex, cell ->
                                key(columnIndex) {
                                    val at = CellAt(rowIndex, columnIndex)
                                    TableCell(
                                        cell,
                                        table.aligns[columnIndex],
                                        header = rowIndex == 0,
                                        context,
                                        focusNow = wanted == at,
                                        onFocused = {
                                            active = at
                                            wanted = null
                                            state.noteFocused(block.id)
                                        },
                                        onChange = { state.updateTable(block.id, table.withCell(rowIndex, columnIndex, it)) },
                                        onMove = { chord ->
                                            val next = nextCell(at, chord, table)
                                            if (next == null) {
                                                false
                                            } else {
                                                if (next.row >= table.rows.size) state.updateTable(block.id, table.withRow(table.rows.lastIndex))
                                                wanted = next
                                                true
                                            }
                                        },
                                        Modifier.width(with(density) { widths[columnIndex].toDp() }),
                                    )
                                }
                            }
                        }
                        if (rowIndex < table.rows.lastIndex) Box(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
                    }
                }
            }
        }
        val cell = active
        if (cell != null && state.focusedBlock == block.id && !context.readOnly) {
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(StretchIcons.Plus, context.strings.addRow, { state.updateTable(block.id, table.withRow(cell.row)) })
                IconButton(StretchIcons.Minus, context.strings.removeRow, { state.updateTable(block.id, table.withoutRow(cell.row)) }, enabled = cell.row > 0 && table.rows.size > 2)
                IconButton(StretchIcons.ChevronRight, context.strings.addColumn, { state.updateTable(block.id, table.withColumn(cell.column)) })
                IconButton(StretchIcons.ChevronLeft, context.strings.removeColumn, { state.updateTable(block.id, table.withoutColumn(cell.column)) }, enabled = table.columns > 1)
            }
        }
    }
}

/** Space on each side of a cell's text. */
private val CELL_PADDING = 8.dp

/** The cell a key moves to from [at], one past the last row for a new one, or null when the key moves nothing. */
private fun nextCell(at: CellAt, chord: String, table: MarkdownTable): CellAt? = when (chord) {
    "Tab" -> if (at.column < table.columns - 1) CellAt(at.row, at.column + 1) else CellAt(at.row + 1, 0)
    "Shift Tab" -> when {
        at.column > 0 -> CellAt(at.row, at.column - 1)
        at.row > 0 -> CellAt(at.row - 1, table.columns - 1)
        else -> null
    }
    "Enter" -> CellAt(at.row + 1, at.column)
    "Up" -> if (at.row > 0) CellAt(at.row - 1, at.column) else null
    "Down" -> if (at.row < table.rows.lastIndex) CellAt(at.row + 1, at.column) else null
    else -> null
}

@Composable
private fun TableCell(
    text: String,
    align: ColumnAlign,
    header: Boolean,
    context: BlockContext,
    focusNow: Boolean,
    onFocused: () -> Unit,
    onChange: (String) -> Unit,
    onMove: (String) -> Boolean,
    modifier: Modifier,
) {
    val c = Stretch.colors
    val look = rememberMarkupLook()
    val state = context.state
    val requester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(TextFieldValue(text)) }
    if (value.text != text) value = TextFieldValue(text, TextRange(text.length))
    val hide = context.readOnly || state.hidesMarkers || !focused
    val transformation = remember(hide, look) { MarkupTransformation(BlockKind.Paragraph, hide, look) }
    LaunchedEffect(focusNow) { if (focusNow) requester.requestFocus() }
    val alignment = when (align) {
        ColumnAlign.Start -> TextAlign.Start
        ColumnAlign.Center -> TextAlign.Center
        ColumnAlign.End -> TextAlign.End
    }
    BasicTextField(
        value = value,
        onValueChange = {
            value = it
            if (it.text != text) onChange(it.text.replace('\n', ' '))
        },
        modifier = modifier
            .focusRequester(requester)
            .holdsTyping()
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused() else state.endTyping()
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (val chord = event.chord()?.toString()) {
                    "Ctrl Z" -> { state.undo(); true }
                    "Ctrl Shift Z", "Ctrl Y" -> { state.redo(); true }
                    null -> false
                    else -> onMove(chord)
                }
            }
            .padding(horizontal = CELL_PADDING, vertical = 5.dp),
        readOnly = context.readOnly,
        textStyle = Stretch.type.body.merge(
            TextStyle(color = c.ink, textAlign = alignment, fontWeight = if (header) FontWeight.SemiBold else null),
        ),
        cursorBrush = SolidColor(c.inkStrong),
        visualTransformation = transformation,
    )
}
