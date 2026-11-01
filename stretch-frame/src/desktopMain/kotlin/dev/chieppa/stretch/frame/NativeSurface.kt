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
 * A native window inside the Compose UI that [renderer] draws [state] into, on a render thread of its own. Each new
 * [state] is handed over for the renderer's next frame, so it should be an immutable snapshot of what to draw. Put it
 * in a [Viewport]'s content to make a GPU viewport.
 *
 * To Compose the surface is an empty box: mouse, pen and touch input over it goes to the Compose UI as anywhere
 * else, so pointer modifiers on a parent (a camera's navigation, the app's tools) work over it unchanged. Compose
 * cannot draw over the surface: what belongs on top of the scene is drawn by the renderer (see [ViewportOverlay]),
 * and popups show over it once [NativeSurfaces] is enabled. Windows only for now; elsewhere [onFailure] gets an
 * [UnsupportedOperationException]. [onFailure] also gets anything the renderer throws, on the UI thread, after which
 * the surface stops drawing.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <S> NativeSurface(
    state: S,
    renderer: SurfaceRenderer<S>,
    modifier: Modifier = Modifier,
    onFailure: (Throwable) -> Unit = {},
) {
    val failure by rememberUpdatedState(onFailure)
    val window = LocalAwtWindow.current
    val background = java.awt.Color(Stretch.colors.surfaceWell.toArgb())
    val canvas = remember(renderer) { SurfaceCanvas(renderer, state) { error -> SwingUtilities.invokeLater { failure(error) } } }
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
