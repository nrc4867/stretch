package dev.chieppa.stretch.gpu.webgpu

/**
 * A WebGPU device: makes buffers, textures, shaders, render and compute pipelines and bind groups, and owns the
 * [queue] that runs commands. Made by a [dev.chieppa.stretch.gpu.GpuContext]'s session; use it from one thread at a time.
 */
expect class GpuDevice : AutoCloseable {
    override fun close()

    val queue: GpuQueue

    fun createBuffer(size: Long, usage: BufferUsage, label: String? = null): GpuBuffer

    fun createTexture(spec: TextureSpec): GpuTexture

    /** Compiles [wgsl]. Mistakes in it show up as errors through the context's error callback. */
    fun createShaderModule(wgsl: String, label: String? = null): GpuShaderModule

    fun createRenderPipeline(spec: RenderPipelineSpec): GpuRenderPipeline

    fun createComputePipeline(spec: ComputePipelineSpec): GpuComputePipeline

    /** Binds [buffers] to the group with [layout]. */
    fun createBindGroup(layout: GpuBindGroupLayout, buffers: List<BufferBinding>, label: String? = null): GpuBindGroup

    fun createCommandEncoder(): GpuCommandEncoder

    /**
     * The first [size] bytes of [buffer], which needs [BufferUsage.MapRead], once the GPU has finished every
     * submitted command that writes it.
     */
    suspend fun readBuffer(buffer: GpuBuffer, size: Long = buffer.size): ByteArray
}
