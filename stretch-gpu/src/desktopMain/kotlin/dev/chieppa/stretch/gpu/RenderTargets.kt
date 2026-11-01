package dev.chieppa.stretch.gpu

import io.ygdrasil.webgpu.Extent3D
import io.ygdrasil.webgpu.GPUTextureUsage
import io.ygdrasil.webgpu.GPUTextureView
import io.ygdrasil.webgpu.TextureDescriptor

/**
 * The frame's own color and depth textures at the output's size: a multisampled color target that resolves into the
 * output when [GpuContext.sampleCount] is above 1, and a depth buffer always. Remade when the size changes.
 */
internal class RenderTargets(private val context: GpuContext) : AutoCloseable {
    private var width = 0
    private var height = 0
    private val owned = mutableListOf<AutoCloseable>()

    /** The multisampled color view, or null when frames draw straight into the output. */
    var color: GPUTextureView? = null
        private set

    var depth: GPUTextureView? = null
        private set

    fun ensure(width: Int, height: Int) {
        if (width == this.width && height == this.height && depth != null) return
        close()
        val size = Extent3D(width.toUInt(), height.toUInt())
        val samples = context.sampleCount.toUInt()
        if (context.sampleCount > 1) color = view(TextureDescriptor(size = size, format = context.format, usage = setOf(GPUTextureUsage.RenderAttachment), sampleCount = samples))
        depth = view(TextureDescriptor(size = size, format = context.depthFormat, usage = setOf(GPUTextureUsage.RenderAttachment), sampleCount = samples))
        this.width = width
        this.height = height
    }

    private fun view(descriptor: TextureDescriptor): GPUTextureView {
        val texture = context.device.createTexture(descriptor)
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
