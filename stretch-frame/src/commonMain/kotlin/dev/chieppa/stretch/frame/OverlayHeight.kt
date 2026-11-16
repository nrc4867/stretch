package dev.chieppa.stretch.frame

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The space a menu or popover leaves between itself and the window's edge when it has to shrink to fit. */
internal val OverlayWindowMargin = 8.dp

/** The least height a menu or popover shrinks to, when its anchor leaves almost no room on either side. */
internal val OverlayMinHeight = 80.dp

/**
 * The tallest a menu or popover may be with [room] beside its anchor, the larger of the spaces below and above it:
 * that room less [OverlayWindowMargin], never under [OverlayMinHeight], and never over the caller's [maxHeight].
 */
internal fun overlayHeightCap(room: Dp, maxHeight: Dp? = null): Dp {
    val fit = (room - OverlayWindowMargin).coerceAtLeast(OverlayMinHeight)
    return if (maxHeight == null) fit else minOf(fit, maxHeight)
}
