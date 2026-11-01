package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import dev.chieppa.stretch.theme.Stretch
import javax.swing.SwingUtilities

/**
 * A native window inside the Compose UI that [renderer] draws [state] into, on a render thread of its own. Each new
 * [state] is handed over for the renderer's next frame, so it should be an immutable snapshot of what to draw. Put it
 * in a [Viewport]'s content to make a GPU viewport.
 *
 * Mouse, pen and touch input over the surface goes to the Compose UI under it, so pointer modifiers on a parent (a
 * camera's navigation, the app's tools) work over it as anywhere else. Compose cannot draw over the surface: what
 * belongs on top of the scene is drawn by the renderer (see [ViewportOverlay]), and popups show over it once
 * [NativeSurfaces] is enabled. Windows only for now; elsewhere [onFailure] gets an [UnsupportedOperationException].
 * [onFailure] also gets anything the renderer throws, on the UI thread, after which the surface stops drawing.
 */
@Composable
fun <S> NativeSurface(
    state: S,
    renderer: SurfaceRenderer<S>,
    modifier: Modifier = Modifier,
    onFailure: (Throwable) -> Unit = {},
) {
    val failure by rememberUpdatedState(onFailure)
    val canvas = remember(renderer) { SurfaceCanvas(renderer, state) { error -> SwingUtilities.invokeLater { failure(error) } } }
    SideEffect { canvas.publish(state) }
    SwingPanel(background = Stretch.colors.surfaceWell, factory = { canvas }, modifier = modifier)
}
