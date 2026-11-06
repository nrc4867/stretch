package dev.chieppa.stretch.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Calls [onPress] with the pointer, in this element's pixels, where a person asks for a context menu: a right-click,
 * or a long press with a pen or finger that stays put. The right-click is used up, so it neither clicks nor selects
 * underneath; a long press leaves the rest of its gesture alone. Pair it with [opensContextMenu] for the keyboard.
 */
fun Modifier.onContextPress(onPress: (Offset) -> Unit): Modifier = composed {
    val press by rememberUpdatedState(onPress)
    pointerInput(Unit) {
        awaitEachGesture {
            var event = awaitPointerEvent(PointerEventPass.Initial)
            while (event.type != PointerEventType.Press) event = awaitPointerEvent(PointerEventPass.Initial)
            val down = event.changes.first()
            if (event.buttons.isSecondaryPressed) {
                down.consume()
                while (true) {
                    val next = awaitPointerEvent(PointerEventPass.Initial)
                    next.changes.forEach { it.consume() }
                    if (next.changes.none { it.pressed }) break
                }
                press(down.position)
                return@awaitEachGesture
            }
            if (down.type == PointerType.Mouse) return@awaitEachGesture
            val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val next = awaitPointerEvent(PointerEventPass.Initial)
                    val change = next.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed || (change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
                }
                true
            }
            if (released == null) press(down.position)
        }
    }
}
