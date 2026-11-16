package dev.chieppa.stretch.frame

import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType

/**
 * Waits for pointer [down] to start a drag: off the element's sides past the touch slop, seen before anything inside
 * it, or with a mouse past the slop, when nothing inside used the move. The change that starts it, or null when the
 * pointer lets go first.
 */
internal suspend fun AwaitPointerEventScope.awaitDragStart(down: PointerInputChange): PointerInputChange? {
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
