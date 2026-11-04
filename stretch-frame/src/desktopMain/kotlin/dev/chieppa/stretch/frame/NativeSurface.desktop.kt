package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import dev.chieppa.stretch.theme.Stretch
import javax.swing.JLayeredPane
import javax.swing.RootPaneContainer
import javax.swing.SwingUtilities
import kotlin.math.roundToInt

/**
 * A native window in the window's layered pane, on a render thread of its own. Compose cannot draw over it: what belongs
 * on top of the scene is drawn by the renderer (see [ViewportOverlay]), and popups show over it once [NativeSurfaces] is
 * enabled. Windows only for now; elsewhere [onFailure] gets an [UnsupportedOperationException].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun <S> NativeSurface(
    state: S,
    renderer: SurfaceRenderer<S>,
    modifier: Modifier,
    picks: SurfacePickQueue<*>?,
    onFailure: (Throwable) -> Unit,
) {
    val failure by rememberUpdatedState(onFailure)
    val window = LocalAwtWindow.current
    val background = java.awt.Color(Stretch.colors.surfaceWell.toArgb())
    val canvas = remember(renderer, picks) {
        SurfaceCanvas(renderer, state, onFailure = { error -> SwingUtilities.invokeLater { failure(error) } }, picks = picks)
    }
    SideEffect {
        canvas.publish(state)
        canvas.background = background
    }
    // The canvas sits in the window's layered pane above the Compose content, not in a SwingPanel: a SwingPanel
    // forwards presses to its component, which would take the rest of a drag away from Compose.
    DisposableEffect(window, canvas) {
        val layers = (window as? RootPaneContainer)?.layeredPane
        if (layers == null) {
            failure(UnsupportedOperationException("A native surface needs a Compose window or dialog"))
            onDispose {}
        } else {
            layers.add(canvas, JLayeredPane.PALETTE_LAYER)
            onDispose {
                layers.remove(canvas)
                layers.repaint()
            }
        }
    }
    Box(
        modifier.onGloballyPositioned { coordinates ->
            val root = window as? RootPaneContainer ?: return@onGloballyPositioned
            val scale = window.graphicsConfiguration?.defaultTransform?.scaleX ?: 1.0
            val origin = SwingUtilities.convertPoint(root.contentPane, 0, 0, root.layeredPane)
            val bounds = coordinates.boundsInWindow()
            canvas.setBounds(
                origin.x + (bounds.left / scale).roundToInt(),
                origin.y + (bounds.top / scale).roundToInt(),
                (bounds.width / scale).roundToInt(),
                (bounds.height / scale).roundToInt(),
            )
        },
    )
}
