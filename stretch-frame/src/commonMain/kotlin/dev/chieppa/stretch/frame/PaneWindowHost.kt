package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp

/**
 * Shows panes torn out of a [PaneGrid] in windows of their own. The platform's window provides one (`StretchWindow`
 * on desktop), and so does each pane window for what is inside it; where there is none, tabs cannot leave the grid
 * and the tab menu does not offer it.
 */
interface PaneWindowHost {
    /**
     * A full window of the app, titled [title], at [bounds]: in the taskbar and the window list, free to sit behind
     * the app's window or on another screen, with the app's title bar, theme and size. Whenever [raise] changes it
     * comes to the front. Moving or resizing it reports [onBoundsChange]; closing it calls [onClose]. While its title
     * bar drags it, [onDragOver] gets the pointer on screen, in the dp of [WindowBounds], and says whether a place
     * there takes the window; it gets null once the pointer leaves such a place or the drag ends. Letting go over one
     * calls [onDrop] with the pointer. [content] draws the whole window, title bar included.
     */
    @Composable
    fun PaneWindowFrame(
        title: String,
        bounds: WindowBounds,
        raise: Int,
        onBoundsChange: (WindowBounds) -> Unit,
        onClose: () -> Unit,
        onDragOver: (screen: Offset?) -> Boolean,
        onDrop: (screen: Offset) -> Unit,
        content: @Composable () -> Unit,
    )

    /**
     * [content], a [size] square, above every window with its top left corner at [at], in the dp of [WindowBounds], or
     * hidden while [at] is null. It follows a drag past the window and never takes focus or input.
     */
    @Composable
    fun DragFloat(at: Offset?, size: Dp, content: @Composable () -> Unit)

    /** Where [point], in this host's window's pixels, is on screen, in the dp of [WindowBounds]. */
    fun screenPosition(point: Offset): Offset
}

/** The host for pane windows of the window this is composed in, or null where tabs cannot leave the grid. */
val LocalPaneWindows = staticCompositionLocalOf<PaneWindowHost?> { null }
