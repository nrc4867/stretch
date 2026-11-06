package dev.chieppa.stretch.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange

/**
 * What a press on a tree row does: [onPress] at once, then a click ([onClick]) or a double-click ([onDoubleClick])
 * when it lets go, or, with a [drag], a drag that moves the row, measured down the whole list in rows [slot] high from
 * row [index], ending in [onDrop] where it lets go. Let go off the row's sides, the drag drops nothing; brought back,
 * it moves the row again. A drag source around the row takes the drag by using up the pointer's changes before the
 * row sees them, as stretch-frame's `paneDragSource` does once the pointer leaves the row's sides.
 */
internal fun Modifier.treeRowPointer(
    rowKey: Any,
    index: Int,
    slot: Float,
    drag: TreeDragState?,
    onPress: () -> Unit,
    onClick: (PointerKeyboardModifiers) -> Unit,
    onDoubleClick: (() -> Unit)?,
    onDrop: (y: Float) -> Unit,
): Modifier = composed {
    val press by rememberUpdatedState(onPress)
    val click by rememberUpdatedState(onClick)
    val doubleClick by rememberUpdatedState(onDoubleClick)
    val drop by rememberUpdatedState(onDrop)
    val rowIndex by rememberUpdatedState(index)
    val rowSlot by rememberUpdatedState(slot)
    var lastClick by remember { mutableStateOf<Long?>(null) }
    pointerInput(rowKey, drag != null) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val mods = currentEvent.keyboardModifiers
            press()
            if (drag != null) {
                var overSlop = 0f
                val slop = awaitTouchSlopOrCancellation(down.id) { change, over -> change.consume(); overSlop = over.y }
                if (slop != null) {
                    drag.key = rowKey
                    drag.away = false
                    drag.y = rowIndex * rowSlot + down.position.y + overSlop
                    val letGo = followRowDrag(drag, rowKey, slop.id)
                    drag.key = null
                    drag.away = false
                    if (letGo) drop(drag.y)
                    return@awaitEachGesture
                }
                if (currentEvent.changes.any { it.pressed }) return@awaitEachGesture
            } else if (waitForUpOrCancellation() == null) {
                return@awaitEachGesture
            }
            val now = currentEvent.changes.first().uptimeMillis
            val twice = doubleClick
            val previous = lastClick
            if (twice != null && previous != null && now - previous < viewConfiguration.doubleTapTimeoutMillis) {
                twice()
                lastClick = null
            } else {
                click(mods)
                lastClick = now
            }
        }
    }
}

/**
 * Follows the drag of row [rowKey] by pointer [pointer] until it lets go: true when it lets go over the tree, to drop
 * there; false when Esc cancelled it, it let go off the row's sides, or a drag source around the row took the pointer
 * by using it up first.
 */
private suspend fun AwaitPointerEventScope.followRowDrag(drag: TreeDragState, rowKey: Any, pointer: PointerId): Boolean {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == pointer } ?: return false
        if (drag.key != rowKey || change.isConsumed) return false
        if (!change.pressed) return !drag.away
        drag.y += change.positionChange().y
        drag.away = change.position.x < 0f || change.position.x > size.width
        change.consume()
    }
}
