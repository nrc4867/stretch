package dev.chieppa.stretch.gpu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import dev.chieppa.stretch.frame.NativeSurface
import dev.chieppa.stretch.frame.SurfacePickQueue
import kotlinx.coroutines.flow.filterNotNull

/** A canvas behind the page that Compose draws over, as [NativeSurface] makes it, drawn by a [WebGpuRenderer]. */
@Composable
actual fun <S> GpuSurface(
    state: S,
    modifier: Modifier,
    picks: SurfacePickQueue<GpuPick>?,
    options: GpuOptions,
    onReady: (description: String) -> Unit,
    onFailure: (Throwable) -> Unit,
    scene: (GpuContext) -> GpuScene<S>,
) {
    val ready by rememberUpdatedState(onReady)
    val makeScene by rememberUpdatedState(scene)
    @Suppress("DEPRECATION")
    val renderer = remember { WebGpuRenderer<S>(options) { makeScene(it) } }
    LaunchedEffect(renderer) {
        snapshotFlow { renderer.description }.filterNotNull().collect { ready(it) }
    }
    NativeSurface(state, renderer, modifier, picks, onFailure)
}
