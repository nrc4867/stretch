package dev.chieppa.stretch.gpu

import androidx.compose.ui.graphics.Color
import dev.chieppa.stretch.frame.SurfaceSize
import dev.chieppa.stretch.frame.ViewportOverlay
import dev.chieppa.stretch.gpu.webgpu.ColorAttachment
import dev.chieppa.stretch.gpu.webgpu.DepthAttachment
import dev.chieppa.stretch.gpu.webgpu.GpuCommandEncoder
import dev.chieppa.stretch.gpu.webgpu.GpuRenderPass
import dev.chieppa.stretch.gpu.webgpu.GpuTextureView
import dev.chieppa.stretch.gpu.webgpu.StoreOp

/**
 * One frame being drawn: its [size], the [encoder] its commands go into, and the targets [beginPass] sets up. A scene
 * may also record copies on [encoder] before or after its render pass.
 */
class GpuFrame internal constructor(
    val context: GpuContext,
    val encoder: GpuCommandEncoder,
    val size: SurfaceSize,
    private val targets: RenderTargets,
    private val output: GpuTextureView,
) {
    val width: Int get() = size.width
    val height: Int get() = size.height

    /**
     * Starts the frame's render pass, cleared to [clear] with a cleared depth buffer. With multisampling it draws into
     * the frame's own target and resolves into the output when the pass ends.
     */
    fun beginPass(clear: Color): GpuRenderPass {
        val multisampled = targets.color
        val color = if (multisampled != null) {
            ColorAttachment(multisampled, resolveTarget = output, store = StoreOp.Discard, clear = clear)
        } else {
            ColorAttachment(output, clear = clear)
        }
        return encoder.beginRenderPass(listOf(color), DepthAttachment(targets.depth ?: error("The frame has no depth target")))
    }

    /** Draws [overlay] over everything drawn so far in [pass]. Call it once per frame, last. */
    fun drawOverlay(pass: GpuRenderPass, overlay: ViewportOverlay) = context.overlay.draw(pass, overlay, width, height)
}
