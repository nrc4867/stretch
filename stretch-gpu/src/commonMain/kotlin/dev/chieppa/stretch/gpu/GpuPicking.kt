package dev.chieppa.stretch.gpu

import dev.chieppa.stretch.frame.SurfaceSize
import dev.chieppa.stretch.gpu.webgpu.BufferUsage
import dev.chieppa.stretch.gpu.webgpu.GpuBuffer
import dev.chieppa.stretch.gpu.webgpu.TextureAspect

/**
 * Picks on the GPU for whatever draws a scene: has the scene draw a state's IDs for one pixel (see
 * [GpuScene.renderIds]) and reads back its ID and depth. Makes its targets the first time it picks. Use it on the
 * thread that renders.
 */
internal class GpuPicking(private val context: GpuContext) : AutoCloseable {
    private var ids: IdTargets? = null
    private var readback: GpuBuffer? = null

    /** What [scene] draws at pixel [x], [y] of [state] drawn at [size]. */
    suspend fun <S> pick(scene: GpuScene<S>, state: S, size: SurfaceSize, x: Int, y: Int): GpuPick? {
        val device = context.device
        val targets = ids ?: IdTargets(context).also { ids = it }
        val buffer = readback
            ?: device.createBuffer(GpuPick.BUFFER_BYTES.toLong(), BufferUsage.CopyDst + BufferUsage.MapRead, "pick readback").also { readback = it }
        device.createCommandEncoder().use { encoder ->
            scene.renderIds(GpuIdFrame(context, encoder, size, x, y, targets.ids.view, targets.depth.view), state)
            encoder.copyTextureToBuffer(targets.ids.texture, buffer, COPY_ROW_ALIGNMENT, bufferOffset = GpuPick.ID_OFFSET.toLong())
            encoder.copyTextureToBuffer(
                targets.depth.texture, buffer, COPY_ROW_ALIGNMENT, bufferOffset = GpuPick.DEPTH_OFFSET.toLong(), aspect = TextureAspect.DepthOnly,
            )
            device.queue.submit(encoder.finish())
        }
        return GpuPick.decode(device.readBuffer(buffer))
    }

    override fun close() {
        readback?.close()
        ids?.close()
        readback = null
        ids = null
    }
}
