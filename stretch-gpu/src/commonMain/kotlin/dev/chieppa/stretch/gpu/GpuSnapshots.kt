package dev.chieppa.stretch.gpu

import dev.chieppa.stretch.frame.SurfaceSize
import dev.chieppa.stretch.gpu.webgpu.BufferUsage
import dev.chieppa.stretch.gpu.webgpu.GpuBuffer
import dev.chieppa.stretch.gpu.webgpu.GpuInstance
import dev.chieppa.stretch.gpu.webgpu.GpuTexture
import dev.chieppa.stretch.gpu.webgpu.GpuTextureView
import dev.chieppa.stretch.gpu.webgpu.TextureFormat
import dev.chieppa.stretch.gpu.webgpu.TextureSpec
import dev.chieppa.stretch.gpu.webgpu.TextureUsage

/** The format snapshots render in; its bytes read back in red, green, blue, alpha order. */
private val SNAPSHOT_FORMAT = TextureFormat.RGBA8Unorm

/**
 * Renders a [GpuScene] into images, one state at a time, on a device and a scene it keeps: for a run of thumbnails, which pays for
 * the device and the scene's pipelines once rather than per image. Each [render] reuses the targets of the last while the size holds.
 * Use it from one thread at a time, and [close] it when the run is over. On the desktop it renders on the calling thread.
 */
class GpuSnapshots<S> private constructor(private val gpu: GpuInstance, private val context: GpuContext, private val scene: GpuScene<S>) : AutoCloseable {
    private val targets = RenderTargets(context)
    private var output: Output? = null

    private class Output(val size: SurfaceSize, val texture: GpuTexture, val view: GpuTextureView, val readback: GpuBuffer, val rowBytes: Int) : AutoCloseable {
        override fun close() {
            readback.close()
            view.close()
            texture.close()
        }
    }

    /** [state] drawn by the scene into a [width] by [height] image at [scale]. */
    suspend fun render(state: S, width: Int, height: Int, scale: Float = 1f): GpuImage {
        require(width > 0 && height > 0) { "An image needs a size" }
        val out = outputFor(SurfaceSize(width, height, scale))
        val device = context.device
        targets.ensure(width, height)
        device.createCommandEncoder().use { encoder ->
            scene.render(GpuFrame(context, encoder, out.size, targets, out.view), state)
            encoder.copyTextureToBuffer(out.texture, out.readback, out.rowBytes)
            device.queue.submit(encoder.finish())
        }
        val bytes = device.readBuffer(out.readback)
        return GpuImage(width, height, argbPixels(bytes, width, height, out.rowBytes, bgra = false))
    }

    private fun outputFor(size: SurfaceSize): Output {
        output?.takeIf { it.size == size }?.let { return it }
        output?.close()
        val device = context.device
        val rowBytes = paddedRowBytes(size.width)
        val texture = device.createTexture(TextureSpec(size.width, size.height, context.format, TextureUsage.RenderAttachment + TextureUsage.CopySrc))
        return Output(size, texture, texture.createView(), device.createBuffer(rowBytes.toLong() * size.height, BufferUsage.CopyDst + BufferUsage.MapRead), rowBytes)
            .also { output = it }
    }

    override fun close() {
        output?.close()
        output = null
        scene.close()
        targets.close()
        context.close()
        gpu.close()
    }

    companion object {
        /** Sets up a device of its own with [options] and makes the [scene] on it. */
        suspend fun <S> open(options: GpuOptions = GpuOptions(), scene: (GpuContext) -> GpuScene<S>): GpuSnapshots<S> {
            val gpu = GpuInstance.create(null, options.backend, options.powerPreference, options.onError)
            val context = try {
                GpuContext(gpu.device, SNAPSHOT_FORMAT, options.sampleCount, gpu.description)
            } catch (failed: Throwable) {
                gpu.close()
                throw failed
            }
            val made = try {
                scene(context)
            } catch (failed: Throwable) {
                context.close()
                gpu.close()
                throw failed
            }
            return GpuSnapshots(gpu, context, made)
        }
    }
}
