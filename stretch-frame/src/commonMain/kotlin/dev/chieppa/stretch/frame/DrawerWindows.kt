package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

/**
 * Shows popped-out drawers in windows of their own. The platform's window provides one (`StretchWindow` on desktop);
 * where there is none, drawers cannot pop out and the menu does not offer it.
 */
interface DrawerWindowHost {
    /**
     * A window holding one drawer, titled [title], at [bounds] or, the first time, inside the app's window on [side]
     * and [defaultWidth] wide. It stays above the app's window and minimizes with it. Whenever [raise] changes it
     * comes to the front. Moving or resizing it reports [onBoundsChange]; closing it calls [onClose]. [content] draws
     * the whole window, header included.
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
        content: @Composable () -> Unit,
    )
}

/** The host for popped-out drawers, or null where drawers cannot leave the frame. */
val LocalDrawerWindows = staticCompositionLocalOf<DrawerWindowHost?> { null }

/**
 * Set by a window whose title bar the app draws: the header at the top of it marks its controls in [regions], reports
 * pointer hits through [onHit] and keeps [start] and [end] free for the OS's buttons.
 */
internal class TitleBarBinding(val regions: TitleBarRegions, val onHit: (Boolean) -> Unit, val start: Dp, val end: Dp)

internal val LocalTitleBar = staticCompositionLocalOf<TitleBarBinding?> { null }
