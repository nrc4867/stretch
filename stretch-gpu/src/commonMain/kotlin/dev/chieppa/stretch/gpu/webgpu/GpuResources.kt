package dev.chieppa.stretch.gpu.webgpu

/** A block of GPU memory, [size] bytes long. */
expect class GpuBuffer : AutoCloseable {
    override fun close()

    val size: Long
}

/** A texture, [width] by [height] pixels in [format]. Draw into it or sample it through a [GpuTextureView]. */
expect class GpuTexture : AutoCloseable {
    override fun close()

    val width: Int
    val height: Int
    val format: TextureFormat

    /** A view of the whole texture. */
    fun createView(): GpuTextureView
}

/** A view of a texture, as render passes and bind groups take it. */
expect class GpuTextureView : AutoCloseable {
    override fun close()
}

/** Compiled WGSL. */
expect class GpuShaderModule : AutoCloseable {
    override fun close()
}

/** The layout of one bind group a pipeline expects. */
expect class GpuBindGroupLayout : AutoCloseable {
    override fun close()
}

/** Resources bound to a pipeline's group, made by [GpuDevice.createBindGroup]. */
expect class GpuBindGroup : AutoCloseable {
    override fun close()
}

/** Recorded commands, ready for [GpuQueue.submit]. */
expect class GpuCommandBuffer : AutoCloseable {
    override fun close()
}

/** A render pipeline: shaders and fixed-function state, made by [GpuDevice.createRenderPipeline]. */
expect class GpuRenderPipeline : AutoCloseable {
    override fun close()

    /** The layout of bind group [index] as the shaders declare it. Close it once the bind group is made. */
    fun bindGroupLayout(index: Int): GpuBindGroupLayout
}
