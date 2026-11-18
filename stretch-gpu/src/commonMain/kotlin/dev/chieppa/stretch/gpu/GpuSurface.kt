package dev.chieppa.stretch.gpu

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.chieppa.stretch.frame.SurfacePickQueue

/**
 * A GPU viewport: [scene], made once on the surface's own device, draws [state] with WebGPU, and the picture is an
 * ordinary part of the Compose UI. Anything Compose draws can sit over it (a [dev.chieppa.stretch.frame.Viewport]'s
 * corners, popups, menus) and moves with it in the same frame. Each new [state] is handed over for the next frame, so it
 * should be an immutable snapshot of what to draw; the scene draws again whenever the state or the size changes, and
 * every frame while it says it animates.
 *
 * On the desktop the scene renders off screen on a thread of its own and each frame is read back into an image the UI
 * draws, about one frame behind the scene being drawn. In the browser it draws into a canvas behind the page, as a
 * [dev.chieppa.stretch.frame.NativeSurface] does. Input over it goes to the Compose UI as anywhere else, so pointer
 * modifiers on [modifier] or a parent work over it unchanged.
 *
 * [options] are read when the surface starts. [onReady] gets the GPU's name and API once set up, and [onFailure]
 * anything that stops the surface, such as a lost device; both run on the UI thread. With [picks], the app can ask what
 * is under a point (see [GpuScene.renderIds]).
 */
@Composable
expect fun <S> GpuSurface(
    state: S,
    modifier: Modifier = Modifier,
    picks: SurfacePickQueue<GpuPick>? = null,
    options: GpuOptions = GpuOptions(),
    onReady: (description: String) -> Unit = {},
    onFailure: (Throwable) -> Unit = {},
    scene: (GpuContext) -> GpuScene<S>,
)
