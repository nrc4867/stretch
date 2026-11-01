package dev.chieppa.stretch.frame

import kotlin.concurrent.thread

/** How long closing waits for the render thread to detach its renderer. */
private const val JOIN_MILLIS = 3000L

/**
 * Runs a [SurfaceRenderer] on a thread of its own: attaches it to [handle], renders the latest published state
 * whenever the state or size changes (and every frame while the renderer asks for more), and detaches it on [close].
 * Anything the renderer throws stops the loop and goes to [onFailure], on the render thread.
 */
internal class SurfaceLoop<S>(
    private val handle: SurfaceHandle,
    private val renderer: SurfaceRenderer<S>,
    initial: S,
    private val onFailure: (Throwable) -> Unit,
) : AutoCloseable {
    private val lock = Object()
    private var state: S = initial
    private var size = SurfaceSize.Zero
    private var dirty = true
    private var running = true

    private val worker = thread(name = "stretch-surface", isDaemon = true) { run() }

    fun publish(state: S) = synchronized(lock) {
        this.state = state
        dirty = true
        lock.notifyAll()
    }

    fun resize(size: SurfaceSize) = synchronized(lock) {
        if (size == this.size) return@synchronized
        this.size = size
        dirty = true
        lock.notifyAll()
    }

    override fun close() {
        synchronized(lock) {
            running = false
            lock.notifyAll()
        }
        if (Thread.currentThread() != worker) worker.join(JOIN_MILLIS)
    }

    private fun run() {
        try {
            renderer.attach(handle)
            try {
                loop()
            } finally {
                renderer.detach()
            }
        } catch (failure: Throwable) {
            onFailure(failure)
        }
    }

    private fun loop() {
        var animating = false
        while (true) {
            val (current, currentSize) = synchronized(lock) {
                while (running && !dirty && !animating) lock.wait()
                if (!running) return
                dirty = false
                state to size
            }
            animating = !currentSize.isEmpty && renderer.render(current, currentSize)
        }
    }
}
