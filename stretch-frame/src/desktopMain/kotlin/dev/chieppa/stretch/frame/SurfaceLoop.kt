package dev.chieppa.stretch.frame

import kotlin.concurrent.thread

/** How long closing waits for the render thread to detach its renderer. */
private const val JOIN_MILLIS = 3000L

/**
 * Runs a [SurfaceRenderer] on a thread of its own: attaches it to [handle], renders the latest published state
 * whenever the state or size changes (and every frame while the renderer asks for more), and detaches it on [close].
 * Anything the renderer throws stops the loop and goes to [onFailure], on the render thread. Picks from [picks] run
 * after the next frame when the renderer is a [SurfacePicker]; a pick that throws fails on its own.
 */
internal class SurfaceLoop<S>(
    private val handle: SurfaceHandle,
    private val renderer: SurfaceRenderer<S>,
    initial: S,
    private val onFailure: (Throwable) -> Unit,
    private val picks: SurfacePickQueue<*>? = null,
) : AutoCloseable {
    private val lock = Object()
    private var state: S = initial
    private var size = SurfaceSize.Zero
    private var dirty = true
    private var running = true
    private var picking = false

    private val worker = thread(name = "stretch-surface", isDaemon = true) { run() }

    init {
        picks?.attach {
            synchronized(lock) {
                picking = true
                lock.notifyAll()
            }
        }
    }

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
        picks?.detach()
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
        // The size of the last frame drawn; a pick looks at that frame.
        var drawn = SurfaceSize.Zero
        while (true) {
            val frame = synchronized(lock) {
                while (running && !dirty && !animating && !picking) lock.wait()
                if (!running) return
                val draw = dirty || animating
                dirty = false
                picking = false
                if (draw) state to size else null
            }
            if (frame != null) {
                val (current, currentSize) = frame
                animating = !currentSize.isEmpty && renderer.render(current, currentSize)
                if (!currentSize.isEmpty) drawn = currentSize
            }
            picks?.let { runPicks(it, drawn) }
        }
    }

    private fun runPicks(queue: SurfacePickQueue<*>, drawn: SurfaceSize) {
        @Suppress("UNCHECKED_CAST")
        val picker = renderer as? SurfacePicker<Any?>
        @Suppress("UNCHECKED_CAST")
        (queue as SurfacePickQueue<Any?>).drain { at ->
            val x = at.x.toInt()
            val y = at.y.toInt()
            if (picker == null || drawn.isEmpty || x !in 0 until drawn.width || y !in 0 until drawn.height) null else picker.pick(x, y)
        }
    }
}
