package dev.chieppa.stretch.ui

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchShadow
import java.awt.Point
import java.awt.Rectangle
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** The room a popup window keeps around its content for the content's shadow. */
internal data class PopupMargins(val left: Dp, val top: Dp, val right: Dp, val bottom: Dp) {
    companion object {
        val None = PopupMargins(0.dp, 0.dp, 0.dp, 0.dp)

        fun around(shadow: StretchShadow?): PopupMargins {
            if (shadow == null) return None
            return PopupMargins(
                left = max(shadow.blur.value - shadow.x.value, 0f).dp,
                top = max(shadow.blur.value - shadow.y.value, 0f).dp,
                right = max(shadow.blur.value + shadow.x.value, 0f).dp,
                bottom = max(shadow.blur.value + shadow.y.value, 0f).dp,
            )
        }
    }
}

/** Keeps a popup of [size] inside a window of [window], at its start when it is too big to fit. */
internal fun clipToWindow(position: IntOffset, size: IntSize, window: IntSize): IntOffset = IntOffset(
    if (size.width < window.width) position.x.coerceIn(0, window.width - size.width) else 0,
    if (size.height < window.height) position.y.coerceIn(0, window.height - size.height) else 0,
)

/**
 * Where a popup window goes, in AWT's units, for content of [size] pixels at [position] pixels inside a window whose
 * content starts at [origin] on screen. Rounds outward, so the content is never cut at a fractional scale.
 */
internal fun popupWindowBounds(origin: Point, position: IntOffset, size: IntSize, margins: PopupMargins, density: Density): Rectangle {
    val scale = density.density
    val left = position.x - margins.left.value * scale
    val top = position.y - margins.top.value * scale
    val right = position.x + size.width + margins.right.value * scale
    val bottom = position.y + size.height + margins.bottom.value * scale
    val x = floor(left / scale).toInt()
    val y = floor(top / scale).toInt()
    return Rectangle(origin.x + x, origin.y + y, ceil(right / scale).toInt() - x, ceil(bottom / scale).toInt() - y)
}

/** Whether [point], in a popup window's units, is on its content and not in the room left for the shadow. */
internal fun isOnContent(point: Point, size: IntSize, margins: PopupMargins, density: Density): Boolean {
    val scale = density.density
    val x = (point.x - margins.left.value) * scale
    val y = (point.y - margins.top.value) * scale
    return x >= 0 && y >= 0 && x < size.width && y < size.height
}
