package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A surface inside the Compose UI that [renderer] draws [state] into. Each new [state] is handed over for the renderer's
 * next frame, so it should be an immutable snapshot of what to draw. Put it in a [Viewport]'s content to make a GPU
 * viewport.
 *
 * To Compose the surface is an empty box: mouse, pen and touch input over it goes to the Compose UI as anywhere else, so
 * pointer modifiers on a parent (a camera's navigation, the app's tools) work over it unchanged. [onFailure] gets
 * anything the renderer throws, on the UI thread, after which the surface stops drawing. With [picks], the app can ask
 * what is under a point (see [SurfacePickQueue]).
 */
@Composable
expect fun <S> NativeSurface(
    state: S,
    renderer: SurfaceRenderer<S>,
    modifier: Modifier = Modifier,
    picks: SurfacePickQueue<*>? = null,
    onFailure: (Throwable) -> Unit = {},
)
