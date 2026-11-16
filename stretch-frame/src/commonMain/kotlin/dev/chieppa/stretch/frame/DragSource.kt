package dev.chieppa.stretch.frame

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Makes this element draggable onto [dropTarget]s in the same window, carrying the item [item] gives. [item] is asked
 * when a drag starts; null means there is nothing to drag. While the drag lasts, [preview] follows the pointer, held
 * where the element was grabbed; draw it lifted, as the thing being moved, such as with `Modifier.lifted`. It is
 * measured as though it stood in the element's place, at most the element's size, so a preview that fills its space
 * comes out the element's size; it draws on nothing of the element's own, so it brings its own fill. It reads the
 * composition locals the element sees, such as the app's theme, though the host draws it. Letting go over a target that takes the
 * item drops it there; letting go anywhere else, or Esc, ends the drag, and the preview goes back to where it started.
 * The drag ends when the pointer lets go, never on a timer.
 *
 * With a mouse the drag starts once the pointer moves past the touch slop with the primary button down; with a pen or
 * finger, which also scroll, once the pointer leaves the element's sides. When something inside the element uses the
 * drag first, such as a list's own reordering, the drag starts only when the pointer leaves the element's sides. It
 * needs a [DragAndDropHost] around it, which every Stretch window has, and does nothing without one.
 */
fun <T : Any> Modifier.dragSource(item: () -> T?, preview: @Composable () -> Unit): Modifier = composed {
    val drags = LocalDragAndDrop.current ?: return@composed Modifier
    val currentItem by rememberUpdatedState(item)
    val currentPreview by rememberUpdatedState(preview)
    val source = remember { SourceSlot() }
    source.preview = currentPreview
    source.locals = currentCompositionLocalContext
    onGloballyPositioned { source.placed = it }.pointerInput(drags) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.type == PointerType.Mouse && !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
            if (drags.session != null) return@awaitEachGesture
            val start = awaitDragStart(down) ?: return@awaitEachGesture
            val thing = currentItem() ?: return@awaitEachGesture
            val at = source.placed?.takeIf { it.isAttached } ?: return@awaitEachGesture
            start.consume()
            val grab = Offset(down.position.x / size.width.coerceAtLeast(1), down.position.y / size.height.coerceAtLeast(1))
            val drag = drags.begin(thing, source.draw, at.size, grab, at.localToWindow(down.position), at.localToWindow(start.position))
            try {
                while (true) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                    if (drags.session !== drag || drag.returning) break
                    change.consume()
                    if (!change.pressed) {
                        drags.drop(drag)
                        break
                    }
                    if (at.isAttached) drags.moveTo(drag, at.localToWindow(change.position))
                }
            } finally {
                drags.cancel(drag)
            }
        }
    }
}

/**
 * Where a source is, the preview it shows and the composition locals it sees, read when that draws so a drag under way
 * shows the latest one.
 */
private class SourceSlot {
    var placed: LayoutCoordinates? = null
    var preview: @Composable () -> Unit = {}
    var locals: CompositionLocalContext? = null
    val draw: @Composable () -> Unit = {
        val seen = locals
        if (seen == null) preview() else CompositionLocalProvider(seen) { preview() }
    }
}
