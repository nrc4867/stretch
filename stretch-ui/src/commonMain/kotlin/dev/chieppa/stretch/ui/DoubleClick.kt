package dev.chieppa.stretch.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Calls [action] on a double-click or double tap, without holding back the single clicks the element takes itself:
 * a `clickable` under it still hears both clicks as they happen. It watches before anything inside, so a click that a
 * control inside uses still counts; a press that moves past the touch slop, such as the start of a drag, is no click.
 */
internal fun Modifier.onDoubleClick(enabled: Boolean, action: () -> Unit): Modifier = if (!enabled) this else composed {
    val current by rememberUpdatedState(action)
    pointerInput(Unit) {
        var lastUp = -1L
        var lastAt = Offset.Zero
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.type == PointerType.Mouse && !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                    lastUp = -1L
                    return@awaitEachGesture
                }
                if (!change.pressed) {
                    val second = lastUp >= 0 &&
                        down.uptimeMillis - lastUp <= viewConfiguration.doubleTapTimeoutMillis &&
                        (down.position - lastAt).getDistance() <= viewConfiguration.touchSlop
                    if (second) {
                        lastUp = -1L
                        current()
                    } else {
                        lastUp = change.uptimeMillis
                        lastAt = change.position
                    }
                    return@awaitEachGesture
                }
            }
        }
    }
}
