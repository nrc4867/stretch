package dev.chieppa.stretch.gpu

import dev.chieppa.stretch.gpu.webgpu.GpuTextureView
import dev.chieppa.stretch.gpu.webgpu.TextureSpec
import dev.chieppa.stretch.gpu.webgpu.TextureUsage

/**
 * The frame's own color and depth textures at the output's size: a multisampled color target that resolves into the
 * output when [GpuContext.sampleCount] is above 1, and a depth buffer always. Remade when the size changes.
 */
internal class RenderTargets(private val context: GpuContext) : AutoCloseable {
    private var width = 0
    private var height = 0
    private val owned = mutableListOf<AutoCloseable>()

    /** The multisampled color view, or null when frames draw straight into the output. */
    var color: GpuTextureView? = null
        private set

    var depth: GpuTextureView? = null
        private set

    fun ensure(width: Int, height: Int) {
        if (width == this.width && height == this.height && depth != null) return
        close()
        if (context.sampleCount > 1) color = view(TextureSpec(width, height, context.format, TextureUsage.RenderAttachment, context.sampleCount))
        depth = view(TextureSpec(width, height, context.depthFormat, TextureUsage.RenderAttachment, context.sampleCount))
        this.width = width
        this.height = height
    }

    private fun view(spec: TextureSpec): GpuTextureView {
        val texture = context.device.createTexture(spec)
        val view = texture.createView()
        owned += view
        owned += texture
        return view
    }

    override fun close() {
        owned.forEach { it.close() }
        owned.clear()
        color = null
        depth = null
        width = 0
        height = 0
    }
}
