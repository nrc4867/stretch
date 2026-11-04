package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp

/**
 * Shows popped-out drawers in windows of their own. The platform's window provides one (`StretchWindow` on desktop);
 * where there is none, drawers cannot pop out and the menu does not offer it.
 */
interface DrawerWindowHost {
    /**
     * A window holding one drawer, titled [title], at [bounds] or, the first time, inside the app's window on [side]
     * and [defaultWidth] wide. It stays above the app's window and minimizes with it. Whenever [raise] changes it
     * comes to the front. Moving or resizing it reports [onBoundsChange]; closing it calls [onClose]. Dragging it
     * over a rail and letting go calls [onDock] with that rail's side and group. [content] draws the whole window,
     * header included.
     */
    @Composable
    fun DrawerWindow(
        title: String,
        bounds: WindowBounds?,
        side: Side,
        defaultWidth: Float,
        raise: Int,
        onBoundsChange: (WindowBounds) -> Unit,
        onClose: () -> Unit,
        onDock: (side: Side, bottom: Boolean) -> Unit,
        content: @Composable () -> Unit,
    )

    /** Where the app's window shows the rail on [side], in its window's pixels, so drawer windows can dock there. */
    fun railBounds(side: Side, bounds: Rect?)

    /**
     * [content], a [size] square, above every window with its top left corner at [at], in the dp of [WindowBounds], or
     * hidden while [at] is null. It follows a drag, including past the app's window, and never takes focus or input.
     */
    @Composable
    fun DragFloat(at: Offset?, size: Dp, content: @Composable () -> Unit)

    /** Where [point], in the app window's pixels, is on screen, in the dp of [WindowBounds]. */
    fun screenPosition(point: Offset): Offset

    /** The rail and group a drawer window would dock into if let go now, while one is dragged over a rail. */
    val dockTarget: DockTarget?
}

data class DockTarget(val side: Side, val bottom: Boolean)

/** The host for popped-out drawers, or null where drawers cannot leave the frame. */
val LocalDrawerWindows = staticCompositionLocalOf<DrawerWindowHost?> { null }

/**
 * Set by a window whose title bar the app draws: the header at the top of it marks its controls in [regions], reports
 * pointer hits through [onHit] and keeps [start] and [end] free for the OS's buttons.
 */
internal class TitleBarBinding(val regions: TitleBarRegions, val onHit: (Boolean) -> Unit, val start: Dp, val end: Dp)

internal val LocalTitleBar = staticCompositionLocalOf<TitleBarBinding?> { null }
