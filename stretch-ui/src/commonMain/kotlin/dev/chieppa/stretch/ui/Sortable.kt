package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key as keyed
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.stretchShadow
import kotlin.math.roundToInt

/** One drag in progress: the item's start index, where the pointer went, and where it would land. */
private class DragSession(val from: Int, val grab: Offset) {
    var pointer by mutableStateOf(grab)
    var target by mutableStateOf(from)
}

private class SortState {
    var session by mutableStateOf<DragSession?>(null)
}

private fun KeyEvent.altArrow(): Key? =
    if (type == KeyEventType.KeyDown && isAltPressed) key.takeIf { it in arrows } else null

private val arrows = setOf(Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight)

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.dragItem(
    state: SortState,
    start: (Offset) -> DragSession?,
    target: (DragSession) -> Int,
    drop: (from: Int, to: Int) -> Unit,
    onStart: () -> Unit,
) = detectDragGestures(
    onDragStart = { at ->
        state.session = start(at)
        if (state.session != null) onStart()
    },
    onDrag = { change: PointerInputChange, amount: Offset ->
        val session = state.session ?: return@detectDragGestures
        change.consume()
        session.pointer += amount
        session.target = target(session)
    },
    onDragEnd = {
        val session = state.session
        state.session = null
        if (session != null && session.target != session.from) drop(session.from, session.target)
    },
    onDragCancel = { state.session = null },
)

@Composable
private fun Modifier.lifted(shape: Shape): Modifier {
    val c = Stretch.colors
    return zIndex(1f).stretchShadow(Stretch.shadows.pop, shape).clip(shape).background(c.surfaceFloat).border(Stretch.stroke, c.lineStrong, shape)
}

/**
 * A list reordered by dragging. With [handles] only the grip drags, so rows keep their own clicks; without, the
 * whole row drags after a short move. Alt Up and Alt Down move the focused row; Esc cancels a drag. Each finished
 * move reports [onMove] once, for the caller to apply as one command.
 */
@Composable
fun <T> SortableList(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    handles: Boolean = true,
    rowHeight: Dp = 32.dp,
    onClick: ((T) -> Unit)? = null,
    content: @Composable RowScope.(T) -> Unit,
) {
    val c = Stretch.colors
    val shape = Stretch.shapes.rowShape
    val gap = 2.dp
    val slot = with(LocalDensity.current) { (rowHeight + gap).toPx() }
    val state = remember { SortState() }
    val current by rememberUpdatedState(items)
    val move by rememberUpdatedState(onMove)
    val listFocus = remember { FocusRequester() }
    val session = state.session

    @Composable
    fun RowBody(item: T, grip: Modifier) = Row(
        Modifier.fillMaxWidth().height(rowHeight).padding(end = 10.dp, start = if (handles) 0.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (handles) Box(grip.width(24.dp).fillMaxHeight(), contentAlignment = Alignment.Center) { Icon(StretchIcons.Drag, c.inkSubtle) }
        content(item)
    }

    Box(
        modifier
            .focusRequester(listFocus)
            .focusable()
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Escape && state.session != null) {
                    state.session = null
                    true
                } else false
            },
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
            items.forEachIndexed { index, item ->
                keyed(key(item)) {
                    val itemKey = key(item)
                    val control = rememberControlState()
                    val drag = remember(itemKey, slot) {
                        Modifier.pointerInput(itemKey, slot) {
                            dragItem(
                                state,
                                start = { at ->
                                    val from = current.indexOfFirst { key(it) == itemKey }
                                    if (from < 0) null else DragSession(from, Offset(at.x, from * slot + at.y))
                                },
                                target = { s -> dropIndex(s.from, s.pointer.y - s.grab.y, slot, current.size) },
                                drop = { from, to -> move(from, to) },
                                onStart = { listFocus.requestFocus() },
                            )
                        }
                    }
                    val ghost = session?.from == index
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .alpha(if (ghost) 0.4f else 1f)
                            .focusRing(control, shape)
                            .clip(shape)
                            .background(control.fill(c.surfacePanel, c.surfaceControl, c.surfaceControlHi))
                            .hoverable(control.source)
                            .then(if (onClick != null) Modifier.clickable(control.source, null, role = Role.Button) { onClick(item) } else Modifier.focusable(interactionSource = control.source))
                            .onKeyEvent { event ->
                                val at = current.indexOfFirst { key(it) == itemKey }
                                val to = when (event.altArrow()) {
                                    Key.DirectionUp -> at - 1
                                    Key.DirectionDown -> at + 1
                                    else -> return@onKeyEvent false
                                }
                                if (to in current.indices) move(at, to)
                                true
                            }
                            .then(if (handles) Modifier else drag),
                    ) { RowBody(item, if (handles) drag else Modifier) }
                }
            }
        }
        if (session != null && session.from in items.indices) {
            val density = LocalDensity.current
            val gapPx = with(density) { gap.toPx() }
            val lineY = (if (session.target > session.from) (session.target + 1) * slot else session.target * slot) - gapPx / 2
            Box(
                Modifier
                    .offset { IntOffset(0, (lineY - 1.dp.toPx()).roundToInt()) }
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(c.ringSelected),
            )
            Box(
                Modifier
                    .offset { IntOffset(8.dp.roundToPx(), (session.pointer.y - session.grab.y + session.from * slot).roundToInt()) }
                    .fillMaxWidth()
                    .lifted(shape),
            ) { RowBody(items[session.from], Modifier) }
        }
    }
}

/**
 * A grid of tiles reordered by dragging the whole tile. The tile it would land on is outlined. Alt and an arrow move
 * the focused tile; Esc cancels a drag. Each finished move reports [onMove] once.
 */
@Composable
fun <T> SortableGrid(
    items: List<T>,
    key: (T) -> Any,
    columns: Int,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    cell: Dp = 72.dp,
    gap: Dp = 8.dp,
    onClick: ((T) -> Unit)? = null,
    content: @Composable BoxScope.(T) -> Unit,
) {
    val c = Stretch.colors
    val shape = Stretch.shapes.cardShape
    val density = LocalDensity.current
    val slot = with(density) { (cell + gap).toPx() }
    val state = remember { SortState() }
    val current by rememberUpdatedState(items)
    val move by rememberUpdatedState(onMove)
    val gridFocus = remember { FocusRequester() }
    val session = state.session
    val rows = (items.size + columns - 1) / columns
    fun origin(index: Int) = Offset((index % columns) * slot, (index / columns) * slot)

    Box(
        modifier
            .size(cell * columns + gap * (columns - 1), cell * rows + gap * (rows - 1).coerceAtLeast(0))
            .focusRequester(gridFocus)
            .focusable()
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Escape && state.session != null) {
                    state.session = null
                    true
                } else false
            },
    ) {
        items.forEachIndexed { index, item ->
            keyed(key(item)) {
                val itemKey = key(item)
                val control = rememberControlState()
                val at = origin(index)
                val target = session != null && session.target == index && session.target != session.from
                Box(
                    Modifier
                        .offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }
                        .size(cell)
                        .alpha(if (session?.from == index) 0.4f else 1f)
                        .focusRing(control, shape)
                        .clip(shape)
                        .background(control.fill(c.surfacePanel, c.surfaceControl, c.surfaceControlHi))
                        .border(if (target) 2.dp else Stretch.stroke, if (target) c.ringSelected else c.linePanel, shape)
                        .hoverable(control.source)
                        .then(if (onClick != null) Modifier.clickable(control.source, null, role = Role.Button) { onClick(item) } else Modifier.focusable(interactionSource = control.source))
                        .onKeyEvent { event ->
                            val from = current.indexOfFirst { key(it) == itemKey }
                            val to = when (event.altArrow()) {
                                Key.DirectionLeft -> from - 1
                                Key.DirectionRight -> from + 1
                                Key.DirectionUp -> from - columns
                                Key.DirectionDown -> from + columns
                                else -> return@onKeyEvent false
                            }
                            if (to in current.indices) move(from, to)
                            true
                        }
                        .then(remember(itemKey, slot, columns) {
                            Modifier.pointerInput(itemKey, slot, columns) {
                                dragItem(
                                    state,
                                    start = { grab ->
                                        val from = current.indexOfFirst { key(it) == itemKey }
                                        if (from < 0) null else DragSession(from, origin(from) + grab)
                                    },
                                    target = { s -> gridIndexAt(s.pointer.x, s.pointer.y, slot, columns, current.size) },
                                    drop = { from, to -> move(from, to) },
                                    onStart = { gridFocus.requestFocus() },
                                )
                            }
                        }),
                    contentAlignment = Alignment.Center,
                ) { content(item) }
            }
        }
        if (session != null && session.from in items.indices) {
            val topLeft = session.pointer - (session.grab - origin(session.from))
            Box(
                Modifier
                    .offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }
                    .size(cell)
                    .lifted(shape),
                contentAlignment = Alignment.Center,
            ) { content(items[session.from]) }
        }
    }
}
