package dev.chieppa.stretch.gpu.readback

import dev.chieppa.stretch.frame.SurfaceSize
import dev.chieppa.stretch.gpu.GpuContext
import dev.chieppa.stretch.gpu.GpuOptions
import dev.chieppa.stretch.gpu.GpuScene
import dev.chieppa.stretch.gpu.argbPixels
import dev.chieppa.stretch.gpu.paddedRowBytes
import dev.chieppa.stretch.gpu.webgpu.GpuBuffer
import dev.chieppa.stretch.gpu.webgpu.TextureFormat
import kotlinx.coroutines.runBlocking

/** [ReadbackMode.Naive]: one frame at a time, each waited for, swizzled and copied as a thumbnail would be. */
internal class NaiveReadbackLoop<S>(
    initial: S,
    private val options: GpuOptions,
    onFailure: (Throwable) -> Unit,
    private val scene: (GpuContext) -> GpuScene<S>,
) : ReadbackLoop<S>(initial, depth = 1, onFailure) {
    private var device: ReadbackDevice<S>? = null
    private var buffer: Pair<SurfaceSize, GpuBuffer>? = null

    override val inFlight: Int get() = 0

    override fun setUp() {
        device = ReadbackDevice(TextureFormat.RGBA8Unorm, options, scene).also { description = it.description }
    }

    override fun submit(state: S, size: SurfaceSize, startedNanos: Long): Boolean {
        val device = checkNotNull(device)
        val target = bufferFor(size)
        val animating = device.render(state, size, target)
        val waitStart = System.nanoTime()
        val bytes = runBlocking { device.context.device.readBuffer(target) }
        val copyStart = System.nanoTime()
        val pixels = argbPixels(bytes, size.width, size.height, paddedRowBytes(size.width), bgra = false)
        val (bitmap, image) = argbFrameImage(size.width, size.height, pixels)
        val done = System.nanoTime()
        stats.gpuWait.addNanos(copyStart - waitStart)
        stats.copy.addNanos(done - copyStart)
        frames.deliver(ReadbackFrame(image, bitmap, frames.nextSerial(), startedNanos))
        stats.delivered++
        return animating
    }

    override fun complete(wait: Boolean) = Unit

    private fun bufferFor(size: SurfaceSize): GpuBuffer {
        buffer?.takeIf { it.first.width == size.width && it.first.height == size.height }?.let { return it.second }
        buffer?.second?.close()
        return checkNotNull(device).bufferFor(size).also { buffer = size to it }
    }

    override fun tearDown() {
        buffer?.second?.close()
        buffer = null
        device?.close()
        device = null
    }
}
