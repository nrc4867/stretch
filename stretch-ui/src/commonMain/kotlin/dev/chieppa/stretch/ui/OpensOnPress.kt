package dev.chieppa.stretch.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach

/**
 * For a control that opens a menu or popover: the mouse or pen opens it on press, not release. Pass it as the control's
 * modifier and keep the same [onOpen] as its click, which still serves touch and the keyboard. A press while it is
 * [open] only closes it, through the popup's click outside.
 */
fun Modifier.opensOnPress(open: Boolean, onOpen: () -> Unit): Modifier = composed {
    // Read as of the last composition: the popup's click outside has already closed it by the time the press gets here.
    val wasOpen by rememberUpdatedState(open)
    val currentOpen by rememberUpdatedState(onOpen)
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.type == PointerType.Touch || !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
            if (!wasOpen) currentOpen()
            // Takes the release from the control's click, which would otherwise reopen what this press closed.
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.fastForEach { if (it.changedToUp()) it.consume() }
            } while (event.changes.fastAny { it.pressed })
        }
    }
}
