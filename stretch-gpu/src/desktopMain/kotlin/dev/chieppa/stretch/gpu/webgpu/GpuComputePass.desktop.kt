package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.gpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment

/** A compute pass being recorded. [end] finishes it; nothing may be recorded on it after that. */
actual class GpuComputePass internal constructor(private val handle: MemorySegment) {
    actual fun setPipeline(pipeline: GpuComputePipeline) = wgpu_h.wgpuComputePassEncoderSetPipeline(handle, pipeline.handle)

    actual fun setBindGroup(index: Int, group: GpuBindGroup) =
        wgpu_h.wgpuComputePassEncoderSetBindGroup(handle, index, group.handle, 0, MemorySegment.NULL)

    /** Runs the pipeline's entry point in [x] by [y] by [z] workgroups, each the size its `@workgroup_size` gives. */
    actual fun dispatchWorkgroups(x: Int, y: Int, z: Int) = wgpu_h.wgpuComputePassEncoderDispatchWorkgroups(handle, x, y, z)

    /**
     * Runs the pipeline in as many workgroups as three unsigned 32-bit integers in [buffer] at [offset] say, written
     * there by the GPU or the CPU. The buffer needs [BufferUsage.Indirect].
     */
    actual fun dispatchWorkgroupsIndirect(buffer: GpuBuffer, offset: Long) =
        wgpu_h.wgpuComputePassEncoderDispatchWorkgroupsIndirect(handle, buffer.handle, offset)

    actual fun end() {
        wgpu_h.wgpuComputePassEncoderEnd(handle)
        wgpu_h.wgpuComputePassEncoderRelease(handle)
    }
}
