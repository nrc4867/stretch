package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import dev.chieppa.stretch.theme.Stretch
import kotlinx.browser.document
import kotlinx.coroutines.CancellationException
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.roundToInt

private const val CANVAS_STYLE = "position:fixed;z-index:-1;left:0;top:0;width:0;height:0"
private const val RGB_MASK = 0xFFFFFF
private const val HEX_DIGITS = 6

private fun Color.toCss(): String = "#" + (toArgb() and RGB_MASK).toString(16).padStart(HEX_DIGITS, '0')

/**
 * A canvas behind the Compose page, seen through a hole this box clears in it. Compose draws over it, so anything in the
 * surface's parent or a popup shows on top, and takes all input over it. It runs on the page's one thread, each frame at
 * the browser's animation frame.
 */
@Composable
actual fun <S> NativeSurface(
    state: S,
    renderer: SurfaceRenderer<S>,
    modifier: Modifier,
    picks: SurfacePickQueue<*>?,
    onFailure: (Throwable) -> Unit,
) {
    val failure by rememberUpdatedState(onFailure)
    val background = Stretch.colors.surfaceWell.toCss()
    val scale = LocalPageDensity.current
    val canvas = remember { document.createElement("canvas") as HTMLCanvasElement }
    val loop = remember(renderer, picks) { BrowserSurfaceLoop(canvas, renderer, state, picks) }
    SideEffect {
        loop.publish(state)
        canvas.style.background = background
    }
    DisposableEffect(canvas) {
        canvas.style.cssText = CANVAS_STYLE
        document.body?.insertBefore(canvas, document.body?.firstChild)
        onDispose { canvas.remove() }
    }
    LaunchedEffect(loop) {
        try {
            loop.run()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            failure(error)
        }
    }
    Box(
        modifier
            .drawWithContent {
                drawRect(Color.Transparent, blendMode = BlendMode.Clear)
                drawContent()
            }
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                canvas.style.left = "${bounds.left / scale}px"
                canvas.style.top = "${bounds.top / scale}px"
                canvas.style.width = "${bounds.width / scale}px"
                canvas.style.height = "${bounds.height / scale}px"
                loop.resize(SurfaceSize(bounds.width.roundToInt(), bounds.height.roundToInt(), scale))
            },
    )
}
