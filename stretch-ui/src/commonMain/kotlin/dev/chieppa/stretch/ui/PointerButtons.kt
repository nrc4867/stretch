@file:OptIn(ExperimentalComposeUiApi::class)

package dev.chieppa.stretch.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Calls [onClick] when the middle button is pressed and let go over this element, as a tab or chip closes on a
 * middle-click. The press and its release are used up, so nothing underneath scrolls or pastes from them.
 */
fun Modifier.onMiddleClick(onClick: () -> Unit): Modifier = composed {
    val click by rememberUpdatedState(onClick)
    pointerInput(Unit) {
        awaitPointerEventScope {
            var pressed = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                when {
                    event.type == PointerEventType.Press && event.button == PointerButton.Tertiary -> {
                        pressed = true
                        event.changes.forEach { it.consume() }
                    }
                    event.type == PointerEventType.Release && event.button == PointerButton.Tertiary && pressed -> {
                        pressed = false
                        event.changes.forEach { it.consume() }
                        val at = event.changes.first().position
                        if (at.x in 0f..size.width.toFloat() && at.y in 0f..size.height.toFloat()) click()
                    }
                }
            }
        }
    }
}

/**
 * Calls [onBack] or [onForward] when the mouse's back or forward button, the fourth and fifth, is pressed over this
 * element, as a browser goes back and forward. Put it on a window's content to hear them anywhere in the window: it
 * sees them before anything inside does, and uses them up.
 */
fun Modifier.onBackForwardButtons(onBack: () -> Unit, onForward: () -> Unit): Modifier = composed {
    val back by rememberUpdatedState(onBack)
    val forward by rememberUpdatedState(onForward)
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type != PointerEventType.Press) continue
                when (event.button) {
                    PointerButton.Back -> back()
                    PointerButton.Forward -> forward()
                    else -> continue
                }
                event.changes.forEach { it.consume() }
            }
        }
    }
}
