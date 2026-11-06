package dev.chieppa.stretch.frame

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity

/**
 * Makes this element draggable into the [PaneGrid] that has [target], as the tab [tab] gives: its id is the tab the
 * grid opens, and its title and icon are what follows the pointer. [tab] is asked when a drag starts; null means
 * there is nothing to drag. The grid previews where it lands and opens it there (see the grid's `dropTarget`).
 *
 * With a mouse the drag starts once the pointer moves past the touch slop with the primary button down; with a pen or
 * finger, which also scroll, once the pointer leaves the element's sides. When something inside the element uses
 * the drag first, such as a [dev.chieppa.stretch.ui.Tree]'s own reordering, the drag starts only when the pointer
 * leaves the element's sides, and takes over from it there.
 */
fun Modifier.paneDragSource(target: PaneDropTarget, tab: () -> TabItem?): Modifier = composed {
    val item by rememberUpdatedState(tab)
    val host = LocalPaneWindows.current
    val toScreen = remember(host) { host?.let { h -> { point: Offset -> h.screenPosition(point) } } ?: SameWindow }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val faceWidth = with(LocalDensity.current) { TabMaxWidth.toPx() }
    onGloballyPositioned { origin = it.positionInWindow() }.pointerInput(target) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.type == PointerType.Mouse && !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
            val start = awaitDragStart(down) ?: return@awaitEachGesture
            val face = item() ?: return@awaitEachGesture
            val drag = target.drag
            start.consume()
            drag.begin(face.id, null, Offset.Zero, origin + start.position, Size(faceWidth, size.height.toFloat()), toScreen, face)
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                if (change == null || drag.tab != face.id) break
                change.consume()
                if (!change.pressed) {
                    target.grid?.finishItem(face.id)
                    break
                }
                drag.moveBy(change.position - change.previousPosition)
            }
            drag.end()
        }
    }
}

/**
 * Waits for pointer [down] to start a drag: off the element's sides past the touch slop, seen before anything inside
 * it, or with a mouse past the slop, when nothing inside used the move. The change that starts it, or null when the
 * pointer lets go first.
 */
private suspend fun AwaitPointerEventScope.awaitDragStart(down: PointerInputChange): PointerInputChange? {
    val mouse = down.type == PointerType.Mouse
    while (true) {
        val early = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: return null
        if (!early.pressed) return null
        val past = (early.position - down.position).getDistance() > viewConfiguration.touchSlop
        if (past && (early.position.x < 0f || early.position.x > size.width)) return early
        val late = awaitPointerEvent(PointerEventPass.Main).changes.firstOrNull { it.id == down.id } ?: return null
        if (mouse && past && !late.isConsumed) return late
    }
}
