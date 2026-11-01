package dev.chieppa.stretch.frame

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Asks a [NativeSurface] what is under a point. The app holds one, hands it to the surface, and calls [pick]; the
 * surface's renderer, which must be a [SurfacePicker] answering [R], runs each pick on its render thread after the
 * next frame. Points are in the surface's pixels from its top-left corner, as Compose positions are on the desktop.
 * A pick answers null when nothing is under the point, the point is off the surface, the renderer cannot pick, or no
 * surface is showing.
 */
class SurfacePickQueue<R> {
    private class Request<R>(val at: Offset, val result: CompletableDeferred<R?>)

    private val pending = ConcurrentLinkedQueue<Request<R>>()

    @Volatile private var wake: (() -> Unit)? = null

    suspend fun pick(at: Offset): R? {
        val request = Request(at, CompletableDeferred<R?>())
        pending += request
        val wake = wake
        if (wake == null) answerNothing() else wake()
        return request.result.await()
    }

    /** Called by the surface's render loop when it starts: [wake] tells it a pick is waiting. */
    internal fun attach(wake: () -> Unit) {
        this.wake = wake
        if (pending.isNotEmpty()) wake()
    }

    /** Called by the surface's render loop when it stops: waiting picks answer null. */
    internal fun detach() {
        wake = null
        answerNothing()
    }

    /** Answers every waiting pick with [answer], or with the exception it throws. */
    internal fun drain(answer: (Offset) -> R?) {
        while (true) {
            val request = pending.poll() ?: return
            runCatching { answer(request.at) }
                .onSuccess { request.result.complete(it) }
                .onFailure { request.result.completeExceptionally(it) }
        }
    }

    private fun answerNothing() = drain { null }
}
