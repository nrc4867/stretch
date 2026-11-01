package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.BadgedBox
import dev.chieppa.stretch.ui.ToolButton
import dev.chieppa.stretch.ui.Tooltip
import kotlin.math.roundToInt

/**
 * A rail button. [bottom] puts it in the group at the foot of the rail. [badge] marks something new or waiting in
 * its drawer: a count, or an empty string for a dot.
 */
data class RailItem(val id: String, val icon: ImageVector, val label: String, val bottom: Boolean = false, val badge: String? = null)

/** Where a dragged rail button lands: its group and its index there, counted without the dragged button. */
data class RailDrop(val bottom: Boolean, val index: Int)

/**
 * The drop for a rail button whose top edge is dragged to [y], on a rail [height] tall holding [top] and [bottom]
 * other buttons, each [slot] tall including the gap, inside [pad] padding.
 */
fun railDrop(y: Float, height: Float, top: Int, bottom: Int, slot: Float, pad: Float): RailDrop {
    val topEnd = pad + top * slot
    val bottomStart = height - pad - bottom * slot
    return if (y + slot / 2 < (topEnd + bottomStart) / 2) {
        RailDrop(false, ((y - pad) / slot).roundToInt().coerceIn(0, top))
    } else {
        RailDrop(true, ((y - bottomStart) / slot).roundToInt().coerceIn(0, bottom))
    }
}

/**
 * The 48px icon rail on one edge. Each button opens or closes one drawer; open drawers show as on. Buttons sit in a
 * top group and a bottom group. With [onMove] they reorder by dragging, between the groups too, or with Alt Up and
 * Alt Down; Esc cancels a drag.
 */
@Composable
fun Rail(
    side: Side,
    items: List<RailItem>,
    open: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    onMove: ((id: String, drop: RailDrop) -> Unit)? = null,
) {
    val c = Stretch.colors
    val density = LocalDensity.current
    val gap = 4.dp
    val padding = 8.dp
    val slot = with(density) { (StretchSizes.tool + gap).toPx() }
    val pad = with(density) { padding.toPx() }
    val gapPx = with(density) { gap.toPx() }
    var height by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf<String?>(null) }
    var grabY by remember { mutableFloatStateOf(0f) }
    var pointerY by remember { mutableFloatStateOf(0f) }
    val current by rememberUpdatedState(items)
    val move by rememberUpdatedState(onMove)
    val railFocus = remember { FocusRequester() }

    fun top(id: String): Float {
        val item = current.first { it.id == id }
        val group = current.filter { it.bottom == item.bottom }
        val index = group.indexOf(item)
        return if (item.bottom) height - pad - (group.size - index) * slot + gapPx else pad + index * slot
    }

    fun drop(id: String): RailDrop {
        val rest = current.filter { it.id != id }
        return railDrop(pointerY - grabY, height, rest.count { !it.bottom }, rest.count { it.bottom }, slot, pad)
    }

    @Composable
    fun button(item: RailItem) {
        val itemId = item.id
        val drag = if (onMove == null) {
            Modifier
        } else {
            Modifier
                .pointerInput(itemId) {
                    detectDragGestures(
                        onDragStart = { at ->
                            dragging = itemId
                            grabY = at.y
                            pointerY = top(itemId) + at.y
                            railFocus.requestFocus()
                        },
                        onDrag = { change, amount ->
                            if (dragging == itemId) {
                                change.consume()
                                pointerY += amount.y
                            }
                        },
                        onDragEnd = {
                            if (dragging == itemId) move?.invoke(itemId, drop(itemId))
                            dragging = null
                        },
                        onDragCancel = { dragging = null },
                    )
                }
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || !event.isAltPressed) return@onKeyEvent false
                    val group = current.filter { it.bottom == item.bottom }
                    val index = group.indexOfFirst { it.id == itemId }
                    val to = when (event.key) {
                        Key.DirectionUp -> index - 1
                        Key.DirectionDown -> index + 1
                        else -> return@onKeyEvent false
                    }
                    if (to in group.indices) move?.invoke(itemId, RailDrop(item.bottom, to))
                    true
                }
        }
        Box(Modifier.alpha(if (dragging == itemId) 0.4f else 1f).then(drag)) {
            Tooltip(item.label) {
                BadgedBox(item.badge, listOfNotNull(item.label, item.badge?.ifEmpty { null }).joinToString(" · ")) {
                    ToolButton(item.icon, item.label, item.id in open, { onToggle(item.id) })
                }
            }
        }
    }

    Row(modifier.fillMaxHeight()) {
        if (side == Side.Right) Spacer(Modifier.fillMaxHeight().width(Stretch.stroke).background(c.linePanel))
        Box(
            Modifier
                .fillMaxHeight()
                .width(StretchSizes.rail)
                .background(c.surfaceRail)
                .onSizeChanged { height = it.height.toFloat() }
                .focusRequester(railFocus)
                .focusable()
                .onKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.Escape && dragging != null) {
                        dragging = null
                        true
                    } else {
                        false
                    }
                },
        ) {
            Column(
                Modifier.fillMaxHeight().fillMaxWidth().padding(vertical = padding),
                verticalArrangement = Arrangement.spacedBy(gap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                for (item in items.filter { !it.bottom }) key(item.id) { button(item) }
                Spacer(Modifier.weight(1f))
                for (item in items.filter { it.bottom }) key(item.id) { button(item) }
            }
            val id = dragging
            val dragged = items.firstOrNull { it.id == id }
            if (id != null && dragged != null) {
                val target = drop(id)
                val rest = items.filter { it.id != id }
                val lineY = if (target.bottom) {
                    height - pad - (rest.count { it.bottom } - target.index) * slot + gapPx / 2
                } else {
                    pad + target.index * slot - gapPx / 2
                }
                Box(
                    Modifier
                        .offset { IntOffset(6.dp.roundToPx(), (lineY - 1.dp.toPx()).roundToInt()) }
                        .width(StretchSizes.rail - 12.dp)
                        .height(2.dp)
                        .background(c.ringSelected),
                )
                Box(
                    Modifier
                        .offset { IntOffset(((StretchSizes.rail - StretchSizes.tool) / 2).roundToPx(), (pointerY - grabY).roundToInt()) }
                        .floating(Stretch.shapes.rowShape, Stretch.shadows.pop),
                ) { ToolButton(dragged.icon, dragged.label, dragged.id in open, {}) }
            }
        }
        if (side == Side.Left) Spacer(Modifier.fillMaxHeight().width(Stretch.stroke).background(c.linePanel))
    }
}
