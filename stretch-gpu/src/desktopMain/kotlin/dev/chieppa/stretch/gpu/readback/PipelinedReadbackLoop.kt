package dev.chieppa.stretch.gpu.readback

import dev.chieppa.stretch.frame.SurfaceSize
import dev.chieppa.stretch.gpu.GpuContext
import dev.chieppa.stretch.gpu.GpuOptions
import dev.chieppa.stretch.gpu.GpuScene
import dev.chieppa.stretch.gpu.paddedRowBytes
import dev.chieppa.stretch.gpu.webgpu.GpuBuffer
import dev.chieppa.stretch.gpu.webgpu.TextureFormat
import dev.chieppa.stretch.gpu.webgpu.mapReadAsync
import dev.chieppa.stretch.gpu.webgpu.mappedBytes
import dev.chieppa.stretch.gpu.webgpu.pollEvents
import dev.chieppa.stretch.gpu.webgpu.unmap
import java.lang.foreign.Arena

/**
 * [ReadbackMode.Pipelined]: frames render in BGRA, Skia's own byte order, into a ring of [depth] buffers that map
 * without waiting, so the GPU draws one frame while the last is read. Each mapped buffer is copied once, straight into
 * the pixels of the frame's image.
 */
internal class PipelinedReadbackLoop<S>(
    initial: S,
    private val depth: Int,
    private val options: GpuOptions,
    onFailure: (Throwable) -> Unit,
    private val scene: (GpuContext) -> GpuScene<S>,
) : ReadbackLoop<S>(initial, depth, onFailure) {
    private class Slot {
        var buffer: GpuBuffer? = null
        var size = SurfaceSize.Zero
        var startedNanos = 0L
        var arena: Arena? = null
        var mapped: Boolean? = null
        var failure: String = ""
    }

    private var device: ReadbackDevice<S>? = null
    private val free = ArrayDeque<Slot>()
    private val submitted = ArrayDeque<Slot>()

    override val inFlight: Int get() = submitted.size

    override fun setUp() {
        device = ReadbackDevice(TextureFormat.BGRA8Unorm, options, scene).also { description = it.description }
        repeat(depth) { free.addLast(Slot()) }
    }

    override fun submit(state: S, size: SurfaceSize, startedNanos: Long): Boolean {
        val device = checkNotNull(device)
        val slot = free.removeFirst()
        if (slot.buffer == null || slot.size.width != size.width || slot.size.height != size.height) {
            slot.buffer?.close()
            slot.buffer = device.bufferFor(size)
        }
        val buffer = checkNotNull(slot.buffer)
        slot.size = size
        slot.startedNanos = startedNanos
        slot.mapped = null
        val animating = device.render(state, size, buffer)
        slot.arena = device.context.device.mapReadAsync(buffer) { ok, message ->
            slot.mapped = ok
            slot.failure = message
        }
        submitted.addLast(slot)
        return animating
    }

    override fun complete(wait: Boolean) {
        val gpu = checkNotNull(device).context.device
        val waitStart = System.nanoTime()
        gpu.pollEvents(wait)
        while (wait && submitted.first().mapped == null) gpu.pollEvents(wait = true)
        if (wait) stats.gpuWait.addNanos(System.nanoTime() - waitStart)
        while (submitted.isNotEmpty() && submitted.first().mapped != null) deliver(submitted.removeFirst())
    }

    private fun deliver(slot: Slot) {
        val buffer = checkNotNull(slot.buffer)
        check(slot.mapped == true) { "Reading a frame back failed: ${slot.failure}" }
        val copyStart = System.nanoTime()
        val rowBytes = paddedRowBytes(slot.size.width)
        val (bitmap, image) = bgraFrameImage(slot.size.width, slot.size.height, rowBytes) { pixels ->
            pixels.copyFrom(buffer.mappedBytes().asSlice(0, pixels.byteSize()))
        }
        buffer.unmap()
        slot.arena?.close()
        slot.arena = null
        stats.copy.addNanos(System.nanoTime() - copyStart)
        frames.deliver(ReadbackFrame(image, bitmap, frames.nextSerial(), slot.startedNanos))
        stats.delivered++
        free.addLast(slot)
    }

    override fun tearDown() {
        val gpu = device?.context?.device
        if (gpu != null && submitted.isNotEmpty()) {
            while (submitted.any { it.mapped == null }) gpu.pollEvents(wait = true)
        }
        (submitted + free).forEach { slot ->
            if (slot.mapped == true) slot.buffer?.unmap()
            slot.arena?.close()
            slot.buffer?.close()
        }
        submitted.clear()
        free.clear()
        device?.close()
        device = null
    }
}
