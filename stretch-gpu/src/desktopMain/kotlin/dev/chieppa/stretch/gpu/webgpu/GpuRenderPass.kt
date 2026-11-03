package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.gpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment

/** A render pass being recorded. [end] finishes it; nothing may be recorded on it after that. */
class GpuRenderPass internal constructor(private val handle: MemorySegment) {
    fun setPipeline(pipeline: GpuRenderPipeline) = wgpu_h.wgpuRenderPassEncoderSetPipeline(handle, pipeline.handle)

    fun setBindGroup(index: Int, group: GpuBindGroup) =
        wgpu_h.wgpuRenderPassEncoderSetBindGroup(handle, index, group.handle, 0, MemorySegment.NULL)

    fun setVertexBuffer(slot: Int, buffer: GpuBuffer, offset: Long = 0, size: Long? = null) =
        wgpu_h.wgpuRenderPassEncoderSetVertexBuffer(handle, slot, buffer.handle, offset, size ?: WHOLE_SIZE)

    fun setIndexBuffer(buffer: GpuBuffer, format: IndexFormat, offset: Long = 0, size: Long? = null) =
        wgpu_h.wgpuRenderPassEncoderSetIndexBuffer(handle, buffer.handle, format.native, offset, size ?: WHOLE_SIZE)

    /** Limits drawing to a rectangle of the target, in pixels. */
    fun setScissorRect(x: Int, y: Int, width: Int, height: Int) = wgpu_h.wgpuRenderPassEncoderSetScissorRect(handle, x, y, width, height)

    /**
     * Maps clip space onto a rectangle of the target, in pixels, so a pass can draw several views with a camera each.
     * Pair it with [setScissorRect] on the same rectangle where a view must stay inside its own.
     */
    fun setViewport(x: Float, y: Float, width: Float, height: Float, minDepth: Float = 0f, maxDepth: Float = 1f) =
        wgpu_h.wgpuRenderPassEncoderSetViewport(handle, x, y, width, height, minDepth, maxDepth)

    fun draw(vertexCount: Int, instanceCount: Int = 1, firstVertex: Int = 0, firstInstance: Int = 0) =
        wgpu_h.wgpuRenderPassEncoderDraw(handle, vertexCount, instanceCount, firstVertex, firstInstance)

    fun drawIndexed(indexCount: Int, instanceCount: Int = 1, firstIndex: Int = 0, baseVertex: Int = 0, firstInstance: Int = 0) =
        wgpu_h.wgpuRenderPassEncoderDrawIndexed(handle, indexCount, instanceCount, firstIndex, baseVertex, firstInstance)

    fun end() {
        wgpu_h.wgpuRenderPassEncoderEnd(handle)
        wgpu_h.wgpuRenderPassEncoderRelease(handle)
    }
}
