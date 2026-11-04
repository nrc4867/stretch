package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.StretchPopup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/** Puts a popup at [offset] from its anchor's top-left corner, whatever its size. */
private data class AtOffset(val offset: IntOffset) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize) =
        anchorBounds.topLeft + offset
}

/** Popups keep their place: they never take focus or close on a click elsewhere. */
private val CornerPopup = PopupProperties(focusable = false, dismissOnBackPress = false, dismissOnClickOutside = false)

/**
 * Shows the viewport's corner [slots] (in [CornerSlot] order) each in a popup of its own, placed by [placeCorners]
 * from the sizes the popups report, so they show over a native surface. A slot shows once its size is known.
 * [onPlaced] gets the area each one covers, in the viewport's pixels.
 */
@Composable
internal fun ViewportCornerPopups(slots: List<(@Composable () -> Unit)?>, lead: Dp, onPlaced: (List<Rect>) -> Unit) {
    val sizes = remember { mutableStateListOf<IntSize?>().apply { repeat(CornerSlot.entries.size) { add(null) } } }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val margins = with(density) { cornerMargins(lead, direction) }
    val shownSizes = slots.indices.map { i -> sizes[i].takeIf { slots[i] != null } }
    val places = placeCorners(shownSizes, viewSize.width, viewSize.height, margins, direction == LayoutDirection.Ltr)
    val room = cornerRoom(viewSize.width, viewSize.height, margins)
    SideEffect { onPlaced(places.mapNotNull { it?.toRect() }) }
    Box(Modifier.fillMaxSize().onSizeChanged { viewSize = it }) {
        if (viewSize == IntSize.Zero) return@Box
        slots.forEachIndexed { i, slot ->
            if (slot == null) return@forEachIndexed
            // Off the window until measured, so a slot never flashes in the wrong place.
            val at = places[i]?.topLeft ?: IntOffset(-viewSize.width, -viewSize.height)
            StretchPopup(popupPositionProvider = AtOffset(at), properties = CornerPopup, shadow = Stretch.shadows.pop) {
                Box(
                    with(density) { Modifier.sizeIn(maxWidth = room.width.toDp(), maxHeight = room.height.toDp()) }
                        .onSizeChanged { sizes[i] = it },
                ) { slot() }
            }
        }
    }
}
