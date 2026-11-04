package dev.chieppa.stretch.gpu

import dev.chieppa.stretch.gpu.webgpu.GpuTexture
import dev.chieppa.stretch.gpu.webgpu.GpuTextureView
import dev.chieppa.stretch.gpu.webgpu.TextureFormat
import dev.chieppa.stretch.gpu.webgpu.TextureSpec
import dev.chieppa.stretch.gpu.webgpu.TextureUsage

/**
 * Picking's targets, one pixel each: the object ID in [GpuContext.idFormat] and the depth in
 * [GpuContext.idDepthFormat], single-sampled and readable by copying. The ID pass draws the picked pixel blown up to
 * fill them (see [pickMatrix]).
 */
internal class IdTargets(context: GpuContext) : AutoCloseable {
    /** A texture and the view a render pass draws into. */
    class Target(val texture: GpuTexture, val view: GpuTextureView) : AutoCloseable {
        override fun close() {
            view.close()
            texture.close()
        }
    }

    val ids = target(context, context.idFormat)
    val depth = target(context, context.idDepthFormat)

    private fun target(context: GpuContext, format: TextureFormat): Target {
        val texture = context.device.createTexture(TextureSpec(1, 1, format, TextureUsage.RenderAttachment + TextureUsage.CopySrc))
        return Target(texture, texture.createView())
    }

    override fun close() {
        ids.close()
        depth.close()
    }
}
