package dev.chieppa.stretch.gpu.webgpu

/** A render pass being recorded. [end] finishes it; nothing may be recorded on it after that. */
expect class GpuRenderPass {
    fun setPipeline(pipeline: GpuRenderPipeline)

    fun setBindGroup(index: Int, group: GpuBindGroup)

    fun setVertexBuffer(slot: Int, buffer: GpuBuffer, offset: Long = 0, size: Long? = null)

    fun setIndexBuffer(buffer: GpuBuffer, format: IndexFormat, offset: Long = 0, size: Long? = null)

    /** Limits drawing to a rectangle of the target, in pixels. */
    fun setScissorRect(x: Int, y: Int, width: Int, height: Int)

    /**
     * Maps clip space onto a rectangle of the target, in pixels, so a pass can draw several views with a camera each.
     * Pair it with [setScissorRect] on the same rectangle where a view must stay inside its own.
     */
    fun setViewport(x: Float, y: Float, width: Float, height: Float, minDepth: Float = 0f, maxDepth: Float = 1f)

    fun draw(vertexCount: Int, instanceCount: Int = 1, firstVertex: Int = 0, firstInstance: Int = 0)

    fun drawIndexed(indexCount: Int, instanceCount: Int = 1, firstIndex: Int = 0, baseVertex: Int = 0, firstInstance: Int = 0)

    fun end()
}
