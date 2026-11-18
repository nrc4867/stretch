package dev.chieppa.stretch.gpu.readback

import dev.chieppa.stretch.frame.SurfaceImplementation
import dev.chieppa.stretch.frame.SurfacePickQueue
import dev.chieppa.stretch.frame.SurfaceSize
import dev.chieppa.stretch.gpu.GpuContext
import dev.chieppa.stretch.gpu.GpuOptions
import dev.chieppa.stretch.gpu.GpuPick
import dev.chieppa.stretch.gpu.GpuScene
import dev.chieppa.stretch.gpu.paddedRowBytes
import dev.chieppa.stretch.gpu.webgpu.GpuBuffer
import dev.chieppa.stretch.gpu.webgpu.mapReadAsync
import dev.chieppa.stretch.gpu.webgpu.mappedBytes
import dev.chieppa.stretch.gpu.webgpu.pollEvents
import dev.chieppa.stretch.gpu.webgpu.unmap
import kotlinx.coroutines.runBlocking
import java.lang.foreign.Arena
import kotlin.concurrent.thread

/** How long closing waits for the render thread to let go of the GPU. */
private const val JOIN_MILLIS = 3000L

/** Frames kept ahead of the UI: one rendering while the last waits to be drawn. */
private const val DEPTH = 2

/**
 * Renders a [GpuScene] off screen on a thread of its own and hands each frame to the UI as an image through [frames].
 * It draws the latest published state whenever it or the size changes, and every frame while the scene animates.
 *
 * Frames go through a ring of [DEPTH] readback buffers that map without waiting, so the GPU draws one frame while the
 * last is read; each mapped buffer is copied once, straight into its image's pixels. The loop starts one frame for each
 * frame the UI draws (and [DEPTH] at first), and never holds more than [DEPTH] frames between rendering and being
 * drawn, which paces it to the UI's frame rate. Picks from [picks] run after the frames that are ready. [onReady] gets
 * the GPU's description once set up; [onFailure] gets anything that stops the loop; [onDelivered] runs after each frame
 * reaches [frames]. All three run on the render thread.
 */
@OptIn(SurfaceImplementation::class)
internal class ReadbackLoop<S>(
    initial: S,
    private val options: GpuOptions,
    private val picks: SurfacePickQueue<GpuPick>?,
    private val onReady: (String) -> Unit,
    private val onFailure: (Throwable) -> Unit,
    private val onDelivered: () -> Unit = {},
    private val scene: (GpuContext) -> GpuScene<S>,
) : AutoCloseable {
    private class Slot {
        var buffer: GpuBuffer? = null
        var size = SurfaceSize.Zero
        var arena: Arena? = null
        var mapped: Boolean? = null
        var failure = ""
    }

    private val lock = Object()
    private var state: S = initial
    private var size = SurfaceSize.Zero
    private var dirty = true
    private var animating = false
    private var running = true
    private var picking = false

    /** Frames that may start before the UI draws another. */
    private var credits = DEPTH

    val frames = FrameHandoff {
        synchronized(lock) {
            if (credits < DEPTH) credits++
            lock.notifyAll()
        }
    }

    // Render thread only from here.
    private val free = ArrayDeque<Slot>()
    private val submitted = ArrayDeque<Slot>()

    /** The size of the newest frame delivered, which picks look at. */
    private var shown = SurfaceSize.Zero

    private val worker = thread(name = "stretch-gpu-surface", isDaemon = true, start = false) { run() }

    fun start() {
        picks?.attach {
            synchronized(lock) {
                picking = true
                lock.notifyAll()
            }
        }
        worker.start()
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

    /** Stops the loop and waits for it, then closes the frames it made. Call it from the UI thread, which then draws none. */
    override fun close() {
        synchronized(lock) {
            running = false
            lock.notifyAll()
        }
        if (Thread.currentThread() != worker) worker.join(JOIN_MILLIS)
        picks?.detach()
        if (!worker.isAlive) frames.closeAll()
    }

    private fun room() = submitted.size + frames.undrawn < DEPTH

    private fun canStart() = (dirty || animating) && !size.isEmpty && credits > 0 && room()

    private fun run() {
        try {
            val device = ReadbackDevice(options, scene)
            repeat(DEPTH) { free.addLast(Slot()) }
            onReady(device.description)
            try {
                loop(device)
            } finally {
                tearDown(device)
            }
        } catch (failure: Throwable) {
            onFailure(failure)
        }
    }

    private fun loop(device: ReadbackDevice<S>) {
        while (true) {
            val (frame, pick) = synchronized(lock) {
                while (running && !canStart() && submitted.isEmpty() && !picking) lock.wait()
                if (!running) return
                val frame = if (canStart()) {
                    dirty = false
                    credits--
                    state to size
                } else {
                    null
                }
                (frame to picking).also { picking = false }
            }
            if (frame != null) submit(device, frame.first, frame.second)
            if (submitted.isNotEmpty()) complete(device, wait = !synchronized(lock) { canStart() })
            frames.retire()
            if (pick) picks?.let { runBlocking { it.answerWith(device, shown) } }
        }
    }

    private fun submit(device: ReadbackDevice<S>, state: S, size: SurfaceSize) {
        val slot = free.removeFirst()
        if (slot.buffer == null || slot.size.width != size.width || slot.size.height != size.height) {
            slot.buffer?.close()
            slot.buffer = device.bufferFor(size)
        }
        val buffer = checkNotNull(slot.buffer)
        slot.size = size
        slot.mapped = null
        animating = device.render(state, size, buffer)
        slot.arena = device.context.device.mapReadAsync(buffer) { ok, message ->
            slot.mapped = ok
            slot.failure = message
        }
        submitted.addLast(slot)
    }

    /** Delivers the frames the GPU has finished; with [wait], first blocks until at least the oldest one is. */
    private fun complete(device: ReadbackDevice<S>, wait: Boolean) {
        val gpu = device.context.device
        gpu.pollEvents(wait)
        while (wait && submitted.first().mapped == null) gpu.pollEvents(wait = true)
        while (submitted.isNotEmpty() && submitted.first().mapped != null) deliver(submitted.removeFirst())
    }

    private fun deliver(slot: Slot) {
        val buffer = checkNotNull(slot.buffer)
        check(slot.mapped == true) { "Reading a frame back failed: ${slot.failure}" }
        val (bitmap, image) = bgraFrameImage(slot.size.width, slot.size.height, paddedRowBytes(slot.size.width)) { pixels ->
            pixels.copyFrom(buffer.mappedBytes().asSlice(0, pixels.byteSize()))
        }
        buffer.unmap()
        slot.mapped = null
        slot.arena?.close()
        slot.arena = null
        shown = slot.size
        frames.deliver(ReadbackFrame(image, bitmap, frames.nextSerial()))
        free.addLast(slot)
        onDelivered()
    }

    private fun tearDown(device: ReadbackDevice<S>) {
        val gpu = device.context.device
        while (submitted.any { it.mapped == null }) gpu.pollEvents(wait = true)
        (submitted + free).forEach { slot ->
            if (slot.mapped == true) slot.buffer?.unmap()
            slot.arena?.close()
            slot.buffer?.close()
        }
        submitted.clear()
        free.clear()
        device.close()
    }
}
