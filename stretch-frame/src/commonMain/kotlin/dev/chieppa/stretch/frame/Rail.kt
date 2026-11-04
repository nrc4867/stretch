package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.BadgedBox
import dev.chieppa.stretch.ui.ToolButton
import dev.chieppa.stretch.ui.Tooltip
import kotlin.math.roundToInt

/**
 * A rail button. [bottom] puts it in the group at the foot of the rail. [badge] marks something new or waiting in
 * its drawer: a count, or an empty string for a dot. [keys] are the key caps its tooltip shows. An item not [enabled]
 * shows faint and opens nothing.
 */
data class RailItem(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val bottom: Boolean = false,
    val badge: String? = null,
    val keys: List<String>? = null,
    val enabled: Boolean = true,
)

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
 * Alt Down; Esc cancels a drag. Rails sharing a [drag] take each other's buttons, so a button dragged across the frame
 * lands on the rail it reaches. A button that [canPopOut] can also be let go away from the rails, which calls
 * [onPopOut] with the pointer in the window's pixels. Where there is a [LocalDrawerWindows] host, a button away from
 * the rails shows in a window of its own, so it stays in sight past the app's window. [windowDrop] lights the top (false) or bottom (true) group while
 * a drawer window dragged over the rail would dock there.
 */
@Composable
fun Rail(
    side: Side,
    items: List<RailItem>,
    open: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    onMove: ((id: String, side: Side, drop: RailDrop) -> Unit)? = null,
    windowDrop: Boolean? = null,
    drag: RailDragState = remember { RailDragState() },
    canPopOut: (String) -> Boolean = { false },
    onPopOut: (id: String, pointer: Offset) -> Unit = { _, _ -> },
) {
    val c = Stretch.colors
    val density = LocalDensity.current
    val windows = LocalDrawerWindows.current
    val gap = 4.dp
    val padding = 8.dp
    val slot = with(density) { (StretchSizes.tool + gap).toPx() }
    val pad = with(density) { padding.toPx() }
    val gapPx = with(density) { gap.toPx() }
    val slack = with(density) { RailDragSlack.toPx() }
    var height by remember { mutableFloatStateOf(0f) }
    val current by rememberUpdatedState(items)
    val move by rememberUpdatedState(onMove)
    val popOut by rememberUpdatedState(onPopOut)
    val poppable by rememberUpdatedState(canPopOut)
    SideEffect { drag.report(side, items) }

    fun target() = drag.target(slot, pad, slack)

    @Composable
    fun button(item: RailItem) {
        val itemId = item.id
        val buttonFocus = remember { FocusRequester() }
        var origin by remember { mutableStateOf(Offset.Zero) }
        val dragModifier = if (onMove == null) {
            Modifier
        } else {
            Modifier
                .onGloballyPositioned { origin = it.positionInWindow() }
                .pointerInput(itemId) {
                    detectDragGestures(
                        onDragStart = { at ->
                            drag.begin(itemId, side, at, origin + at)
                            buttonFocus.requestFocus()
                        },
                        onDrag = { change, amount ->
                            if (drag.id == itemId) {
                                change.consume()
                                drag.moveBy(amount)
                            }
                        },
                        onDragEnd = {
                            if (drag.id == itemId) {
                                when (val target = target()) {
                                    is RailDragTarget.Slot -> move?.invoke(itemId, target.side, target.drop)
                                    RailDragTarget.Away -> if (poppable(itemId)) popOut(itemId, drag.pointer)
                                }
                            }
                            drag.end()
                        },
                        onDragCancel = { drag.end() },
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
                    if (to in group.indices) move?.invoke(itemId, side, RailDrop(item.bottom, to))
                    true
                }
        }
        Box(Modifier.alpha(if (drag.id == itemId) 0.4f else 1f).focusRequester(buttonFocus).then(dragModifier)) {
            Tooltip(item.label, item.keys) {
                BadgedBox(item.badge, listOfNotNull(item.label, item.badge?.ifEmpty { null }).joinToString(" · ")) {
                    ToolButton(item.icon, item.label, item.id in open, { onToggle(item.id) }, enabled = item.enabled)
                }
            }
        }
    }

    // The dragged button travels over the rest of the frame, so its rail draws above its neighbours meanwhile.
    Row(modifier.zIndex(if (drag.id != null && drag.from == side) 1f else 0f).fillMaxHeight()) {
        if (side == Side.Right) Spacer(Modifier.fillMaxHeight().width(Stretch.stroke).background(c.linePanel))
        Box(
            Modifier
                .fillMaxHeight()
                .width(StretchSizes.rail)
                .background(c.surfaceRail)
                .onSizeChanged { height = it.height.toFloat() }
                .onGloballyPositioned { drag.rails[side] = it.boundsInWindow() }
                // Esc reaches this from the dragged button, which takes the focus when a drag starts.
                .onKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.Escape && drag.id != null) {
                        drag.end()
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
            if (windowDrop != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .align(if (windowDrop) Alignment.BottomCenter else Alignment.TopCenter)
                        .padding(4.dp)
                        .background(c.ringSelected.copy(alpha = 0.14f), Stretch.shapes.rowShape)
                        .border(2.dp, c.ringSelected, Stretch.shapes.rowShape),
                )
            }
            val id = drag.id
            if (id != null) {
                val target = target()
                if (target is RailDragTarget.Slot && target.side == side) {
                    val rest = items.filter { it.id != id }
                    val lineY = if (target.drop.bottom) {
                        height - pad - (rest.count { it.bottom } - target.drop.index) * slot + gapPx / 2
                    } else {
                        pad + target.drop.index * slot - gapPx / 2
                    }
                    Box(
                        Modifier
                            .offset { IntOffset(6.dp.roundToPx(), (lineY - 1.dp.toPx()).roundToInt()) }
                            .width(StretchSizes.rail - 12.dp)
                            .height(2.dp)
                            .background(c.ringSelected),
                    )
                }
                val dragged = items.firstOrNull { it.id == id }
                val origin = drag.rails[side]?.topLeft ?: Offset.Zero
                if (drag.from == side && dragged != null) {
                    val floatingButton = @Composable {
                        Box(Modifier.floating(Stretch.shapes.rowShape, Stretch.shadows.pop)) {
                            ToolButton(dragged.icon, dragged.label, dragged.id in open, {})
                        }
                    }
                    val topLeft = drag.pointer - drag.grab
                    val away = target == RailDragTarget.Away
                    // Made when the drag starts and shown once the button leaves the rails, so it is ready by then.
                    windows?.DragFloat(if (away) windows.screenPosition(topLeft) else null, StretchSizes.tool, floatingButton)
                    if (!away || windows == null) {
                        // On a rail the button lines up with it; away from them it follows the pointer.
                        val railX = (target as? RailDragTarget.Slot)?.let { drag.rails[it.side]?.center?.x }
                        Box(
                            Modifier.offset {
                                val x = if (railX != null) railX - StretchSizes.tool.toPx() / 2 else topLeft.x
                                IntOffset((x - origin.x).roundToInt(), (topLeft.y - origin.y).roundToInt())
                            },
                        ) { floatingButton() }
                    }
                }
            }
        }
        if (side == Side.Left) Spacer(Modifier.fillMaxHeight().width(Stretch.stroke).background(c.linePanel))
    }
}
