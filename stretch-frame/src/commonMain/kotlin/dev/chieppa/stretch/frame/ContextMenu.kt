package dev.chieppa.stretch.frame

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

/** Places a popup at [point] inside its anchor, flipped left or up when it would leave the window. */
private class AtPoint(private val point: IntOffset) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = anchorBounds.left + point.x
        val y = anchorBounds.top + point.y
        return IntOffset(
            (if (x + popupContentSize.width > windowSize.width) x - popupContentSize.width else x).coerceAtLeast(0),
            (if (y + popupContentSize.height > windowSize.height) y - popupContentSize.height else y).coerceAtLeast(0),
        )
    }
}

/**
 * Gives [content] a context menu: a right-click, or a long press with a pen or finger, opens [menu] at the pointer.
 * Put [MenuItem]s inside and call `close` from each. Every item should also exist in the command palette, so the
 * menu is a shortcut, never the only way.
 */
@Composable
fun ContextMenuArea(
    menu: @Composable ColumnScope.(close: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var at by remember { mutableStateOf<Offset?>(null) }
    Box(
        modifier.pointerInput(Unit) {
            awaitEachGesture {
                var press = awaitPointerEvent(PointerEventPass.Initial)
                while (press.type != PointerEventType.Press) press = awaitPointerEvent(PointerEventPass.Initial)
                val down = press.changes.first()
                if (press.buttons.isSecondaryPressed) {
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                        if (event.changes.none { it.pressed }) break
                    }
                    at = down.position
                    return@awaitEachGesture
                }
                if (down.type == PointerType.Mouse) return@awaitEachGesture
                val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed || (change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
                    }
                    true
                }
                if (released == null) at = down.position
            }
        },
    ) {
        content()
        val point = at
        if (point != null) {
            val close = { at = null }
            Popup(
                popupPositionProvider = AtPoint(IntOffset(point.x.roundToInt(), point.y.roundToInt())),
                onDismissRequest = close,
                properties = PopupProperties(focusable = true),
            ) { MenuPanel { menu(close) } }
        }
    }
}
