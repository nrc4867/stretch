package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

/** The space between a [PickerGrid]'s cells. */
val PickerGridGap = 8.dp

/**
 * A grid of things to pick from, drawn by [cell], with a keyboard cursor. The grid is one tab stop: arrow keys move
 * [cursor] over the cells, skipping those not [enabled], and Enter picks the cell under it, as a double-click on a cell
 * does; a click puts the cursor on a cell. The cursor shows as a ring in the focus color over its cell wherever the
 * focus is, so a search field above the grid can move it while the person types; [cell] hears whether it holds the
 * cursor, and draws a cell that is not enabled as faint. Columns of at least [minCell] reflow with the width, as
 * [Grid]'s do, and the grid scrolls the cursor's cell into view in the scroll container it sits in.
 *
 * Up from the first row calls [onLeaveTop] and Down from the last row [onLeaveBottom], for passing the cursor and the
 * focus on, such as up into a search field or down into the next group's grid. Give the grid a
 * `Modifier.focusRequester` to bring the focus to it. Enter with no cursor is left to the content around the grid, such
 * as a panel's default button. [contentDescription] names the grid for assistive tools.
 */
@Composable
fun <T> PickerGrid(
    items: List<T>,
    cursor: T?,
    onCursorChange: (T?) -> Unit,
    onPick: (T) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    minCell: Dp = 80.dp,
    enabled: (T) -> Boolean = { true },
    onLeaveTop: () -> Unit = {},
    onLeaveBottom: () -> Unit = {},
    cell: @Composable (item: T, cursor: Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val rtl = isRtl()
    var columns by remember { mutableIntStateOf(1) }
    val current by rememberUpdatedState(cursor)
    Grid(
        modifier
            .onSizeChanged { size -> columns = with(density) { gridColumns(size.width.toFloat(), minCell.toPx(), PickerGridGap.toPx()) } }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val move = when (along(event.key, rtl)) {
                    Key.DirectionLeft -> GridMove.Left
                    Key.DirectionRight -> GridMove.Right
                    Key.DirectionUp -> GridMove.Up
                    Key.DirectionDown -> GridMove.Down
                    Key.Enter, Key.NumPadEnter -> {
                        val item = current?.takeIf { it in items && enabled(it) } ?: return@onKeyEvent false
                        onPick(item)
                        return@onKeyEvent true
                    }
                    else -> return@onKeyEvent false
                }
                val from = items.indexOf(current).takeIf { it >= 0 }
                when (val step = gridStep(from, items.size, columns, move) { enabled(items[it]) }) {
                    is GridStep.To -> onCursorChange(items[step.index])
                    GridStep.LeaveTop -> onLeaveTop()
                    GridStep.LeaveBottom -> onLeaveBottom()
                    GridStep.Stay -> Unit
                }
                true
            }
            .focusable()
            .semantics { this.contentDescription = contentDescription },
        minCell = minCell,
        gap = PickerGridGap,
    ) {
        items.forEach { item ->
            key(item) {
                val on = item == cursor
                PickerGridCell(
                    cursor = on,
                    enabled = enabled(item),
                    onClick = { onCursorChange(item) },
                    onDoubleClick = { onPick(item) },
                ) { cell(item, on) }
            }
        }
    }
}

/** One cell of a [PickerGrid]: the hover and press fill under [content], the cursor ring over it. */
@Composable
private fun PickerGridCell(cursor: Boolean, enabled: Boolean, onClick: () -> Unit, onDoubleClick: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberControlState()
    val shape = Stretch.shapes.rowShape
    val fill = if (enabled) Fills.borderless(state) else Color.Transparent
    val (edgeWidth, edge) = pickEdge(Stretch.colors.surfacePanel, cursor)
    val reveal = remember { BringIntoViewRequester() }
    LaunchedEffect(cursor) { if (cursor) reveal.bringIntoView() }
    Box(
        Modifier
            .bringIntoViewRequester(reveal)
            .clip(shape)
            .background(fill)
            .focusProperties { canFocus = false }
            .hoverable(state.source, enabled)
            .onDoubleClick(enabled, onDoubleClick)
            .controlPointer().clickable(state.source, null, enabled, role = Role.Button, onClick = onClick)
            .semantics { selected = cursor }
            .border(edgeWidth, edge, shape),
    ) { content() }
}
