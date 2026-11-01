package dev.chieppa.stretch.gpu

import dev.chieppa.stretch.frame.SurfaceSize
import dev.chieppa.stretch.gpu.webgpu.BufferUsage
import dev.chieppa.stretch.gpu.webgpu.GpuInstance
import dev.chieppa.stretch.gpu.webgpu.TextureFormat
import dev.chieppa.stretch.gpu.webgpu.TextureSpec
import dev.chieppa.stretch.gpu.webgpu.TextureUsage

/** The format snapshots render in; its bytes read back in red, green, blue, alpha order. */
private val SNAPSHOT_FORMAT = TextureFormat.RGBA8Unorm

/**
 * Renders a [GpuScene] once into an image instead of a window: for exporting a still, for thumbnails, and for checking
 * what a scene draws without opening one. Runs on the calling thread with a device of its own, so it is slow to start
 * and meant for single images rather than frames.
 */
object GpuSnapshot {
    fun <S> render(
        state: S,
        width: Int,
        height: Int,
        scale: Float = 1f,
        options: GpuOptions = GpuOptions(),
        scene: (GpuContext) -> GpuScene<S>,
    ): GpuImage {
        require(width > 0 && height > 0) { "An image needs a size" }
        GpuInstance.create(null, options.backend, options.powerPreference, options.onError).use { gpu ->
            GpuContext(gpu.device, SNAPSHOT_FORMAT, options.sampleCount, gpu.description).use { context ->
                RenderTargets(context).use { targets ->
                    scene(context).use { drawn ->
                        return render(context, targets, drawn, state, SurfaceSize(width, height, scale))
                    }
                }
            }
        }
    }

    private fun <S> render(context: GpuContext, targets: RenderTargets, scene: GpuScene<S>, state: S, size: SurfaceSize): GpuImage {
        val device = context.device
        val rowBytes = paddedRowBytes(size.width)
        val usage = TextureUsage.RenderAttachment + TextureUsage.CopySrc
        device.createTexture(TextureSpec(size.width, size.height, context.format, usage)).use { output ->
            device.createBuffer(rowBytes.toLong() * size.height, BufferUsage.CopyDst + BufferUsage.MapRead).use { readback ->
                output.createView().use { view ->
                    targets.ensure(size.width, size.height)
                    device.createCommandEncoder().use { encoder ->
                        scene.render(GpuFrame(context, encoder, size, targets, view), state)
                        encoder.copyTextureToBuffer(output, readback, rowBytes)
                        device.queue.submit(encoder.finish())
                    }
                }
                val bytes = device.readBuffer(readback)
                return GpuImage(size.width, size.height, argbPixels(bytes, size.width, size.height, rowBytes, bgra = false))
            }
        }
    }
}
