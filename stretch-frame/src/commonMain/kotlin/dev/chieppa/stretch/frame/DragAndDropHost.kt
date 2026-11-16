package dev.chieppa.stretch.frame

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.StretchPopup
import kotlin.math.abs
import kotlin.math.max

/** How long a dragged item's preview takes to go back when nothing takes it. */
private const val RETURN_MILLIS = 160

/**
 * Lets [dragSource] items in [content] be dragged onto [dropTarget]s in it: it keeps their [DragAndDrop], draws the
 * dragged item's preview over [content], and ends a drag on Esc while anything in [content] has the focus. Put it at
 * the root of a window; `StretchWindow`, its pane and drawer windows, and `stretchPage` do already. The preview draws
 * under popups, so a drag shows over the window's own content; where popups show over native surfaces it is a popup
 * itself, so it shows over those too, with room around it for its shadow. Either way the preview is measured at most
 * its source's size.
 */
@Composable
fun DragAndDropHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val drags = remember { DragAndDrop() }
    var host by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        modifier
            .onGloballyPositioned { host = it }
            .onPreviewKeyEvent { event ->
                val drag = drags.session
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape && drag != null && !drag.returning) {
                    drags.cancel(drag)
                    true
                } else {
                    false
                }
            },
        propagateMinConstraints = true,
    ) {
        CompositionLocalProvider(LocalDragAndDrop provides drags) { content() }
        drags.session?.let { drag -> DragPreview(drags, drag, host) }
    }
}

/** The preview of [drag], under the pointer while it drags and going back to where it started once it has ended untaken. */
@Composable
private fun DragPreview(drags: DragAndDrop, drag: DragSession, host: LayoutCoordinates?) {
    val back = remember(drag) { Animatable(drag.pointer, Offset.VectorConverter) }
    LaunchedEffect(drag, drag.returning) {
        if (!drag.returning) return@LaunchedEffect
        back.snapTo(drag.pointer)
        back.animateTo(drag.start, tween(RETURN_MILLIS))
        drags.finish(drag)
    }
    val pointer = if (drag.returning) back.value else drag.pointer
    if (dragPreviewInPopup) {
        val inHost = host?.takeIf { it.isAttached }?.windowToLocal(pointer) ?: pointer
        val room = previewRoom()
        val roomPx = with(LocalDensity.current) { room.roundToPx() }
        StretchPopup(UnderPointer(inHost, drag.grab, roomPx), properties = PreviewPopup) {
            Box(Modifier.padding(room).sourceSized(drag.size)) { drag.preview() }
        }
        return
    }
    Box(
        Modifier
            .zIndex(1f)
            .layout { measurable, _ ->
                val placeable = measurable.measure(atMost(drag.size))
                layout(0, 0) {
                    val inHost = host?.takeIf { it.isAttached }?.windowToLocal(pointer) ?: pointer
                    placeable.place(previewTopLeft(inHost, drag.grab, Size(placeable.width.toFloat(), placeable.height.toFloat())).round())
                }
            },
    ) { drag.preview() }
}

/** Constraints that let content be at most [size], whatever room it is given. */
private fun atMost(size: IntSize) = Constraints(maxWidth = size.width.coerceAtLeast(0), maxHeight = size.height.coerceAtLeast(0))

/** Measures the content at most [size], whatever room it is given, as a preview stands in its source's place. */
private fun Modifier.sourceSized(size: IntSize) = layout { measurable, _ ->
    val placeable = measurable.measure(atMost(size))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/**
 * The room a preview popup keeps clear around the preview, which draws past its own bounds: its shadow, and a slight
 * turn an app may give it.
 */
@Composable
private fun previewRoom(): Dp {
    val shadow = Stretch.shadows.pop
    return shadow.blur + max(abs(shadow.x.value), abs(shadow.y.value)).dp + TURN_ROOM
}

/** Room for the corners of a preview turned a few degrees. */
private val TURN_ROOM = 6.dp

/** A preview popup: it never takes the keyboard or closes itself; the drag ends it. */
private val PreviewPopup = PopupProperties(focusable = false, dismissOnBackPress = false, dismissOnClickOutside = false)

/**
 * Places a preview popup held at [grab] under [pointer], given in the host's own coordinates, its preview inset by
 * [room] pixels on every side.
 */
internal class UnderPointer(private val pointer: Offset, private val grab: Offset, private val room: Int = 0) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val preview = Size((popupContentSize.width - 2 * room).coerceAtLeast(0).toFloat(), (popupContentSize.height - 2 * room).coerceAtLeast(0).toFloat())
        return anchorBounds.topLeft + previewTopLeft(pointer, grab, preview).round() - IntOffset(room, room)
    }
}
