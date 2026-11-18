package dev.chieppa.stretch.gpu.readback

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import dev.chieppa.stretch.frame.SurfaceSize
import org.jetbrains.skia.Rect

/**
 * Draws [source]'s frames as an ordinary part of the Compose tree, so anything Compose draws, popups included, can sit
 * over it. The source renders at the element's size, or at [renderSize] pixels when given, scaled to fit.
 */
@Composable
fun <S> ReadbackSurface(state: S, source: ReadbackSource<S>, modifier: Modifier = Modifier, renderSize: IntSize? = null) {
    val density = LocalDensity.current.density
    SideEffect {
        source.publish(state)
        if (renderSize != null) source.resize(SurfaceSize(renderSize.width, renderSize.height, density))
    }
    Box(
        modifier
            .onSizeChanged { if (renderSize == null) source.resize(SurfaceSize(it.width, it.height, density)) }
            .drawBehind {
                val frame = source.frames.latest.value ?: return@drawBehind
                val start = System.nanoTime()
                drawIntoCanvas { it.nativeCanvas.drawImageRect(frame.image, Rect.makeWH(size.width, size.height)) }
                source.stats.uiDraw.addNanos(System.nanoTime() - start)
                source.stats.latency.addNanos(System.nanoTime() - frame.startedNanos)
                source.stats.draws++
                source.frames.consumed(frame)
            },
    )
}
