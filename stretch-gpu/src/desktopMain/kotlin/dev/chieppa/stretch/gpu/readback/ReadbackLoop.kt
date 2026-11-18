package dev.chieppa.stretch.gpu.readback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.chieppa.stretch.frame.SurfaceSize
import kotlin.concurrent.thread

/** How long closing waits for the render thread to let go of the GPU. */
private const val JOIN_MILLIS = 3000L

/**
 * The render thread behind a [ReadbackSource]: draws the latest state whenever it or the size changes, and every frame
 * while the scene animates, keeping at most [depth] frames ahead of the UI (in flight on the GPU or delivered but not
 * yet drawn). It starts one frame per frame the UI draws, from [depth] at first, which paces it to the UI's frame rate:
 * a frame renders while the last one waits to be drawn. Subclasses [submit] frames and [complete] the ones the GPU has
 * finished.
 */
internal abstract class ReadbackLoop<S>(initial: S, private val depth: Int, private val onFailure: (Throwable) -> Unit) : ReadbackSource<S> {
    private val lock = Object()
    private var state: S = initial
    private var size = SurfaceSize.Zero
    private var dirty = true
    private var animating = false
    private var running = true

    /** Frames that may start before the UI draws another. */
    private var credits = depth

    final override val stats = ReadbackStats()
    final override val frames = FrameHandoff {
        synchronized(lock) {
            if (credits < depth) credits++
            lock.notifyAll()
        }
    }
    final override var description: String? by mutableStateOf(null)
        protected set

    /** Frames submitted and not yet delivered. Render thread only. */
    protected abstract val inFlight: Int

    private val worker = thread(name = "stretch-readback", isDaemon = true, start = false) { run() }

    fun start() = apply { worker.start() }

    final override fun publish(state: S) = synchronized(lock) {
        this.state = state
        dirty = true
        lock.notifyAll()
    }

    final override fun resize(size: SurfaceSize) = synchronized(lock) {
        if (size == this.size) return@synchronized
        this.size = size
        dirty = true
        lock.notifyAll()
    }

    final override fun close() {
        synchronized(lock) {
            running = false
            lock.notifyAll()
        }
        if (Thread.currentThread() != worker) worker.join(JOIN_MILLIS)
    }

    protected abstract fun setUp()

    /** Starts drawing [state] at [size], which began at [startedNanos]; returns whether the scene animates. */
    protected abstract fun submit(state: S, size: SurfaceSize, startedNanos: Long): Boolean

    /** Delivers frames the GPU has finished; with [wait], blocks until at least the oldest one is. */
    protected abstract fun complete(wait: Boolean)

    protected abstract fun tearDown()

    private fun room() = inFlight + frames.undrawn < depth

    private fun canStart() = (dirty || animating) && !size.isEmpty && credits > 0 && room()

    private fun run() {
        try {
            setUp()
            try {
                loop()
            } finally {
                frames.closeAll()
                tearDown()
            }
        } catch (failure: Throwable) {
            onFailure(failure)
        }
    }

    private fun loop() {
        while (true) {
            val frame = synchronized(lock) {
                while (running && !canStart() && inFlight == 0) lock.wait()
                if (!running) return
                if (canStart()) {
                    dirty = false
                    credits--
                    state to size
                } else {
                    null
                }
            }
            if (frame != null) animating = submit(frame.first, frame.second, System.nanoTime())
            if (inFlight > 0) complete(wait = !synchronized(lock) { canStart() })
            frames.retire()
        }
    }
}
