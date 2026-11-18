package dev.chieppa.stretch.gpu

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import dev.chieppa.stretch.frame.SurfacePickQueue
import dev.chieppa.stretch.frame.SurfaceSize
import dev.chieppa.stretch.gpu.readback.ReadbackLoop
import javax.swing.SwingUtilities

/**
 * Reads the scene back into Compose. Each frame is drawn at its own pixel size from the top-left corner, clipped to the
 * surface, so while the surface resizes the last frame shows unstretched until one at the new size arrives.
 */
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
    val failure by rememberUpdatedState(onFailure)
    val makeScene by rememberUpdatedState(scene)
    val loop = remember(picks) {
        ReadbackLoop(
            state,
            options,
            picks,
            onReady = { description -> SwingUtilities.invokeLater { ready(description) } },
            onFailure = { error -> SwingUtilities.invokeLater { failure(error) } },
            scene = { makeScene(it) },
        )
    }
    DisposableEffect(loop) {
        loop.start()
        onDispose { loop.close() }
    }
    SideEffect { loop.publish(state) }
    val density = LocalDensity.current.density
    Box(
        modifier
            .onSizeChanged { loop.resize(SurfaceSize(it.width, it.height, density)) }
            .drawBehind {
                val frame = loop.frames.latest.value ?: return@drawBehind
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    native.save()
                    native.clipRect(org.jetbrains.skia.Rect.makeWH(size.width, size.height))
                    native.drawImage(frame.image, 0f, 0f)
                    native.restore()
                }
                loop.frames.drawn(frame)
            },
    )
}
