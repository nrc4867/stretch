package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.chieppa.stretch.theme.Stretch
import kotlinx.coroutines.delay

/** How long the pointer rests on something before its tooltip shows, in milliseconds. */
const val TOOLTIP_DELAY_MS = 500L

/** The gap between a tooltip and what it is about, in pixels. */
private const val TOOLTIP_GAP = 6

/**
 * Centers a popup over its anchor, above it when there is room. With [area], it centers over that rectangle within the
 * anchor instead, in pixels from the anchor's top left.
 */
private class AboveAnchor(private val gap: Int, private val area: IntRect? = null) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val target = area?.translate(anchorBounds.topLeft) ?: anchorBounds
        val x = (target.center.x - popupContentSize.width / 2).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val above = target.top - gap - popupContentSize.height
        val y = if (above >= 0) above else target.bottom + gap
        return IntOffset(x, y)
    }
}

/**
 * The tooltip: the one place for a full sentence. Names the action and its key, shown after a short hover.
 * Long-press with a pen arrives with the input layer.
 */
@Composable
fun Tooltip(text: String, keys: List<String>? = null, content: @Composable () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(hovered) {
        if (hovered) {
            delay(TOOLTIP_DELAY_MS)
            visible = true
        } else {
            visible = false
        }
    }
    Box(Modifier.hoverable(source)) {
        content()
        if (visible) {
            StretchPopup(popupPositionProvider = AboveAnchor(TOOLTIP_GAP), properties = PopupProperties(focusable = false)) { TooltipBubble(text, keys) }
        }
    }
}

/**
 * A tooltip shown now over [bounds], a rectangle in pixels from the top left of the element this is composed in, for a
 * view that finds for itself what the pointer rests on, such as a run of code. Below it when there is no room above.
 */
@Composable
fun TooltipAt(text: String, bounds: Rect) {
    val area = bounds.roundToIntRect()
    StretchPopup(popupPositionProvider = remember(area) { AboveAnchor(TOOLTIP_GAP, area) }, properties = PopupProperties(focusable = false)) {
        TooltipBubble(text, null)
    }
}

@Composable
private fun TooltipBubble(text: String, keys: List<String>?) {
    val c = Stretch.colors
    Row(
        Modifier
            .clip(Stretch.shapes.ctlShape)
            .background(c.inkStrong)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(text, style = Stretch.type.bodySm.merge(TextStyle(color = c.surfacePanel)))
        if (keys != null) Kbd(keys)
    }
}
