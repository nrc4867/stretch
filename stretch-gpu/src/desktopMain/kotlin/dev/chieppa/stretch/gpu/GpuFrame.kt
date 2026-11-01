package dev.chieppa.stretch.gpu

import androidx.compose.ui.graphics.Color
import dev.chieppa.stretch.frame.SurfaceSize
import dev.chieppa.stretch.frame.ViewportOverlay
import io.ygdrasil.webgpu.GPUCommandEncoder
import io.ygdrasil.webgpu.GPULoadOp
import io.ygdrasil.webgpu.GPURenderPassEncoder
import io.ygdrasil.webgpu.GPUStoreOp
import io.ygdrasil.webgpu.GPUTextureView
import io.ygdrasil.webgpu.RenderPassColorAttachment
import io.ygdrasil.webgpu.RenderPassDepthStencilAttachment
import io.ygdrasil.webgpu.RenderPassDescriptor
import io.ygdrasil.webgpu.Color as GpuColor

/**
 * One frame being drawn: its [size], the [encoder] its commands go into, and the targets [beginPass] sets up. A scene
 * may also record compute passes and copies on [encoder] before or after its render pass.
 */
class GpuFrame internal constructor(
    val context: GpuContext,
    val encoder: GPUCommandEncoder,
    val size: SurfaceSize,
    private val targets: RenderTargets,
    private val output: GPUTextureView,
) {
    val width: Int get() = size.width
    val height: Int get() = size.height

    /**
     * Starts the frame's render pass, cleared to [clear] with a cleared depth buffer. With multisampling it draws into
     * the frame's own target and resolves into the output when the pass ends.
     */
    fun beginPass(clear: Color): GPURenderPassEncoder {
        val multisampled = targets.color
        return encoder.beginRenderPass(
            RenderPassDescriptor(
                colorAttachments = listOf(
                    RenderPassColorAttachment(
                        view = multisampled ?: output,
                        resolveTarget = if (multisampled != null) output else null,
                        loadOp = GPULoadOp.Clear,
                        storeOp = if (multisampled != null) GPUStoreOp.Discard else GPUStoreOp.Store,
                        clearValue = GpuColor(clear.red.toDouble(), clear.green.toDouble(), clear.blue.toDouble(), clear.alpha.toDouble()),
                    ),
                ),
                depthStencilAttachment = RenderPassDepthStencilAttachment(
                    view = targets.depth ?: error("The frame has no depth target"),
                    depthClearValue = 1f,
                    depthLoadOp = GPULoadOp.Clear,
                    depthStoreOp = GPUStoreOp.Discard,
                ),
            ),
        )
    }

    /** Draws [overlay] over everything drawn so far in [pass]. Call it once per frame, last. */
    fun drawOverlay(pass: GPURenderPassEncoder, overlay: ViewportOverlay) = context.overlay.draw(pass, overlay, width, height)
}
