package dev.chieppa.stretch.gpu.webgpu

/** A compute pass being recorded. [end] finishes it; nothing may be recorded on it after that. */
expect class GpuComputePass {
    fun setPipeline(pipeline: GpuComputePipeline)

    fun setBindGroup(index: Int, group: GpuBindGroup)

    /** Runs the pipeline's entry point in [x] by [y] by [z] workgroups, each the size its `@workgroup_size` gives. */
    fun dispatchWorkgroups(x: Int, y: Int = 1, z: Int = 1)

    /**
     * Runs the pipeline in as many workgroups as three unsigned 32-bit integers in [buffer] at [offset] say, written
     * there by the GPU or the CPU. The buffer needs [BufferUsage.Indirect].
     */
    fun dispatchWorkgroupsIndirect(buffer: GpuBuffer, offset: Long = 0)

    fun end()
}
