package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.gpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment

/** A render pass being recorded. [end] finishes it; nothing may be recorded on it after that. */
actual class GpuRenderPass internal constructor(private val handle: MemorySegment) {
    actual fun setPipeline(pipeline: GpuRenderPipeline) = wgpu_h.wgpuRenderPassEncoderSetPipeline(handle, pipeline.handle)

    actual fun setBindGroup(index: Int, group: GpuBindGroup) =
        wgpu_h.wgpuRenderPassEncoderSetBindGroup(handle, index, group.handle, 0, MemorySegment.NULL)

    actual fun setVertexBuffer(slot: Int, buffer: GpuBuffer, offset: Long, size: Long?) =
        wgpu_h.wgpuRenderPassEncoderSetVertexBuffer(handle, slot, buffer.handle, offset, size ?: WHOLE_SIZE)

    actual fun setIndexBuffer(buffer: GpuBuffer, format: IndexFormat, offset: Long, size: Long?) =
        wgpu_h.wgpuRenderPassEncoderSetIndexBuffer(handle, buffer.handle, format.native, offset, size ?: WHOLE_SIZE)

    /** Limits drawing to a rectangle of the target, in pixels. */
    actual fun setScissorRect(x: Int, y: Int, width: Int, height: Int) = wgpu_h.wgpuRenderPassEncoderSetScissorRect(handle, x, y, width, height)

    /**
     * Maps clip space onto a rectangle of the target, in pixels, so a pass can draw several views with a camera each.
     * Pair it with [setScissorRect] on the same rectangle where a view must stay inside its own.
     */
    actual fun setViewport(x: Float, y: Float, width: Float, height: Float, minDepth: Float, maxDepth: Float) =
        wgpu_h.wgpuRenderPassEncoderSetViewport(handle, x, y, width, height, minDepth, maxDepth)

    actual fun draw(vertexCount: Int, instanceCount: Int, firstVertex: Int, firstInstance: Int) =
        wgpu_h.wgpuRenderPassEncoderDraw(handle, vertexCount, instanceCount, firstVertex, firstInstance)

    actual fun drawIndexed(indexCount: Int, instanceCount: Int, firstIndex: Int, baseVertex: Int, firstInstance: Int) =
        wgpu_h.wgpuRenderPassEncoderDrawIndexed(handle, indexCount, instanceCount, firstIndex, baseVertex, firstInstance)

    actual fun end() {
        wgpu_h.wgpuRenderPassEncoderEnd(handle)
        wgpu_h.wgpuRenderPassEncoderRelease(handle)
    }
}
