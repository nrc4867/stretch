package dev.chieppa.stretch.gpu

import dev.chieppa.stretch.frame.SurfaceSize
import io.ygdrasil.webgpu.BufferDescriptor
import io.ygdrasil.webgpu.Extent3D
import io.ygdrasil.webgpu.GPUBufferUsage
import io.ygdrasil.webgpu.GPUMapMode
import io.ygdrasil.webgpu.GPUTextureFormat
import io.ygdrasil.webgpu.GPUTextureUsage
import io.ygdrasil.webgpu.TexelCopyBufferInfo
import io.ygdrasil.webgpu.TexelCopyTextureInfo
import io.ygdrasil.webgpu.TextureDescriptor
import io.ygdrasil.webgpu.mapInto
import io.ygdrasil.webgpu.poll
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

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
        GpuSession.offscreen(options).use { session ->
            GpuContext(session.device, session.format, options.sampleCount, session.description).use { context ->
                RenderTargets(context).use { targets ->
                    scene(context).use { drawn ->
                        return render(session, context, targets, drawn, state, SurfaceSize(width, height, scale))
                    }
                }
            }
        }
    }

    private fun <S> render(
        session: GpuSession,
        context: GpuContext,
        targets: RenderTargets,
        scene: GpuScene<S>,
        state: S,
        size: SurfaceSize,
    ): GpuImage {
        val device = session.device
        val extent = Extent3D(size.width.toUInt(), size.height.toUInt())
        val output = device.createTexture(
            TextureDescriptor(size = extent, format = session.format, usage = setOf(GPUTextureUsage.RenderAttachment, GPUTextureUsage.CopySrc)),
        )
        val outputView = output.createView()
        val rowBytes = paddedRowBytes(size.width)
        val readback = device.createBuffer(
            BufferDescriptor(size = (rowBytes.toLong() * size.height).toULong(), usage = setOf(GPUBufferUsage.CopyDst, GPUBufferUsage.MapRead)),
        )
        try {
            targets.ensure(size.width, size.height)
            val encoder = device.createCommandEncoder()
            scene.render(GpuFrame(context, encoder, size, targets, outputView), state)
            encoder.copyTextureToBuffer(
                TexelCopyTextureInfo(texture = output),
                TexelCopyBufferInfo(buffer = readback, bytesPerRow = rowBytes.toUInt(), rowsPerImage = size.height.toUInt()),
                extent,
            )
            val commands = encoder.finish()
            device.queue.submit(listOf(commands))
            commands.close()
            encoder.close()

            val bytes = ByteArray(rowBytes * size.height)
            runBlocking {
                // wgpu only finishes a map while the device is polled.
                val mapped = async { readback.mapAsync(setOf(GPUMapMode.Read)) }
                while (!mapped.isCompleted) {
                    device.poll()
                    yield()
                }
                mapped.await().getOrThrow()
            }
            readback.mapInto(bytes)
            readback.unmap()
            return GpuImage(size.width, size.height, argbPixels(bytes, size.width, size.height, rowBytes, bgra = session.format == GPUTextureFormat.BGRA8Unorm))
        } finally {
            readback.close()
            outputView.close()
            output.close()
        }
    }
}
