package dev.chieppa.stretch.frame

import kotlinx.browser.window
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLCanvasElement
import kotlin.coroutines.resume

/**
 * Runs a [SurfaceRenderer] on the page's one thread: attaches it to [canvas], and at each animation frame renders the
 * latest published state if the state or size changed or the renderer asked for more. Picks from [picks] run after the
 * frame. [run] returns only by being cancelled, which detaches the renderer; what the renderer throws comes out of it.
 */
internal class BrowserSurfaceLoop<S>(
    private val canvas: HTMLCanvasElement,
    private val renderer: SurfaceRenderer<S>,
    initial: S,
    private val picks: SurfacePickQueue<*>?,
) {
    private var state = initial
    private var size = SurfaceSize.Zero
    private var dirty = true
    private var picking = false
    private val wake = Channel<Unit>(Channel.CONFLATED)

    fun publish(state: S) {
        this.state = state
        dirty = true
        wake.trySend(Unit)
    }

    fun resize(size: SurfaceSize) {
        if (size == this.size) return
        this.size = size
        canvas.width = size.width
        canvas.height = size.height
        dirty = true
        wake.trySend(Unit)
    }

    suspend fun run() {
        renderer.attach(SurfaceHandle.Canvas(canvas))
        picks?.attach {
            picking = true
            wake.trySend(Unit)
        }
        try {
            var animating = false
            var drawn = SurfaceSize.Zero
            while (true) {
                if (!dirty && !animating && !picking) wake.receive()
                animationFrame()
                val draw = dirty || animating
                dirty = false
                picking = false
                if (draw) {
                    animating = !size.isEmpty && renderer.render(state, size)
                    if (!size.isEmpty) drawn = size
                }
                picks?.answerWith(renderer, drawn)
            }
        } finally {
            picks?.detach()
            renderer.detach()
        }
    }

    private suspend fun animationFrame() = suspendCancellableCoroutine { continuation ->
        val id = window.requestAnimationFrame { if (continuation.isActive) continuation.resume(Unit) }
        continuation.invokeOnCancellation { window.cancelAnimationFrame(id) }
    }
}
