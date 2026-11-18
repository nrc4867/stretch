package dev.chieppa.stretch.gpu.readback

import dev.chieppa.stretch.frame.SurfacePicker
import dev.chieppa.stretch.frame.SurfaceSize
import dev.chieppa.stretch.gpu.GpuContext
import dev.chieppa.stretch.gpu.GpuFrame
import dev.chieppa.stretch.gpu.GpuOptions
import dev.chieppa.stretch.gpu.GpuPick
import dev.chieppa.stretch.gpu.GpuPicking
import dev.chieppa.stretch.gpu.GpuScene
import dev.chieppa.stretch.gpu.RenderTargets
import dev.chieppa.stretch.gpu.paddedRowBytes
import dev.chieppa.stretch.gpu.webgpu.BufferUsage
import dev.chieppa.stretch.gpu.webgpu.GpuBuffer
import dev.chieppa.stretch.gpu.webgpu.GpuInstance
import dev.chieppa.stretch.gpu.webgpu.GpuTexture
import dev.chieppa.stretch.gpu.webgpu.GpuTextureView
import dev.chieppa.stretch.gpu.webgpu.TextureFormat
import dev.chieppa.stretch.gpu.webgpu.TextureSpec
import dev.chieppa.stretch.gpu.webgpu.TextureUsage
import kotlinx.coroutines.runBlocking

/** Frames render in BGRA: Skia's own byte order, so the bytes read back go into an image as they are. */
private val FRAME_FORMAT = TextureFormat.BGRA8Unorm

/**
 * A device with no window, drawing a [GpuScene] into a texture and copying it into readback buffers, and picking in
 * what it last drew. Made and used on the render thread only.
 */
internal class ReadbackDevice<S>(options: GpuOptions, scene: (GpuContext) -> GpuScene<S>) : SurfacePicker<GpuPick>, AutoCloseable {
    private val gpu = runBlocking { GpuInstance.create(null, options.backend, options.powerPreference, options.onError) }
    val context = GpuContext(gpu.device, FRAME_FORMAT, options.sampleCount, gpu.description)
    val description: String get() = gpu.description
    private val targets = RenderTargets(context)
    private val scene = scene(context)
    private val picking = GpuPicking(context)
    private var output: Pair<GpuTexture, GpuTextureView>? = null
    private var outputSize = SurfaceSize.Zero

    /** The last state drawn and its size, which a pick draws again. */
    private var last: Pair<S, SurfaceSize>? = null

    /** A buffer that holds one [size] frame, rows padded as copies need. */
    fun bufferFor(size: SurfaceSize): GpuBuffer =
        context.device.createBuffer(paddedRowBytes(size.width).toLong() * size.height, BufferUsage.CopyDst + BufferUsage.MapRead, "frame readback")

    /** Records and submits [state] drawn at [size] and its copy into [into]; returns whether the scene animates. */
    fun render(state: S, size: SurfaceSize, into: GpuBuffer): Boolean {
        gpu.lost?.let { error(it) }
        val (texture, view) = outputFor(size)
        targets.ensure(size.width, size.height)
        last = state to size
        val device = context.device
        return device.createCommandEncoder().use { encoder ->
            val animating = scene.render(GpuFrame(context, encoder, size, targets, view), state)
            encoder.copyTextureToBuffer(texture, into, paddedRowBytes(size.width))
            device.queue.submit(encoder.finish())
            animating
        }
    }

    override suspend fun pick(x: Int, y: Int): GpuPick? {
        val (state, size) = last ?: return null
        return picking.pick(scene, state, size, x, y)
    }

    private fun outputFor(size: SurfaceSize): Pair<GpuTexture, GpuTextureView> {
        output?.takeIf { size.width == outputSize.width && size.height == outputSize.height }?.let { return it }
        closeOutput()
        val texture = context.device.createTexture(
            TextureSpec(size.width, size.height, context.format, TextureUsage.RenderAttachment + TextureUsage.CopySrc, label = "frame"),
        )
        outputSize = size
        return (texture to texture.createView()).also { output = it }
    }

    private fun closeOutput() {
        output?.let { (texture, view) ->
            view.close()
            texture.close()
        }
        output = null
    }

    override fun close() {
        closeOutput()
        picking.close()
        scene.close()
        targets.close()
        context.close()
        gpu.close()
    }
}
