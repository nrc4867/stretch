package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.StretchPopup
import dev.chieppa.stretch.ui.TransientPopup
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
 * A context menu open at [at], in pixels from the top left of the element this is composed in, or nothing while [at]
 * is null. It flips left or up to stay in the window. Clicking away or Esc calls [onDismiss]; [menu] gets the same
 * to call from each [MenuItem]. For a view that finds its own rows or items under the pointer, such as a `Tree`'s
 * `onContextMenu`, where [ContextMenuArea] around the whole view would not know which was clicked.
 */
@Composable
fun ContextMenuPopup(at: Offset?, onDismiss: () -> Unit, menu: @Composable ColumnScope.(close: () -> Unit) -> Unit) {
    if (at == null) return
    StretchPopup(
        popupPositionProvider = AtPoint(IntOffset(at.x.roundToInt(), at.y.roundToInt())),
        onDismissRequest = onDismiss,
        properties = TransientPopup,
        shadow = Stretch.shadows.pop,
    ) { MenuPanel { menu(onDismiss) } }
}
