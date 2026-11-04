package dev.chieppa.stretch.frame

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlin.concurrent.Volatile

/**
 * Asks a [NativeSurface] what is under a point. The app holds one, hands it to the surface, and calls [pick]; the
 * surface's renderer, which must be a [SurfacePicker] answering [R], runs each pick after the next frame. Points are in
 * the surface's pixels from its top-left corner, as Compose positions are. A pick answers null when nothing is under
 * the point, the point is off the surface, the renderer cannot pick, or no surface is showing.
 */
class SurfacePickQueue<R> {
    private class Request<R>(val at: Offset, val result: CompletableDeferred<R?>)

    private val pending = Channel<Request<R>>(Channel.UNLIMITED)

    @Volatile private var wake: (() -> Unit)? = null

    suspend fun pick(at: Offset): R? {
        val request = Request(at, CompletableDeferred<R?>())
        pending.trySend(request)
        val wake = wake
        if (wake == null) answerNothing() else wake()
        return request.result.await()
    }

    /** Called by the surface's render loop when it starts: [wake] tells it a pick is waiting. */
    internal fun attach(wake: () -> Unit) {
        this.wake = wake
        wake()
    }

    /** Called by the surface's render loop when it stops: waiting picks answer null. */
    internal fun detach() {
        wake = null
        answerNothing()
    }

    /** Answers every waiting pick through [renderer], which has drawn a frame of size [drawn]. */
    internal suspend fun answerWith(renderer: SurfaceRenderer<*>, drawn: SurfaceSize) {
        @Suppress("UNCHECKED_CAST")
        val picker = renderer as? SurfacePicker<R>
        while (true) {
            val request = pending.tryReceive().getOrNull() ?: return
            val x = request.at.x.toInt()
            val y = request.at.y.toInt()
            val inside = !drawn.isEmpty && x in 0 until drawn.width && y in 0 until drawn.height
            runCatching { if (picker == null || !inside) null else picker.pick(x, y) }
                .onSuccess { request.result.complete(it) }
                .onFailure { request.result.completeExceptionally(it) }
        }
    }

    private fun answerNothing() {
        while (true) {
            val request = pending.tryReceive().getOrNull() ?: return
            request.result.complete(null)
        }
    }
}
