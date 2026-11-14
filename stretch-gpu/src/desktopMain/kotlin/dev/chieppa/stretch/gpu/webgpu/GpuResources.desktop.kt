package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.gpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment

/** A block of GPU memory, [size] bytes long. */
actual class GpuBuffer internal constructor(handle: MemorySegment, actual val size: Long) : GpuHandle(handle, wgpu_h::wgpuBufferRelease)

/** A texture, [width] by [height] pixels in [format]. Draw into it or sample it through a [GpuTextureView]. */
actual class GpuTexture internal constructor(handle: MemorySegment, actual val width: Int, actual val height: Int, actual val format: TextureFormat) :
    GpuHandle(handle, wgpu_h::wgpuTextureRelease) {
    /** A view of the whole texture. */
    actual fun createView(): GpuTextureView = GpuTextureView(wgpu_h.wgpuTextureCreateView(handle, MemorySegment.NULL))
}

/** A view of a texture, as render passes and bind groups take it. */
actual class GpuTextureView internal constructor(handle: MemorySegment) : GpuHandle(handle, wgpu_h::wgpuTextureViewRelease)

/** Compiled WGSL. */
actual class GpuShaderModule internal constructor(handle: MemorySegment) : GpuHandle(handle, wgpu_h::wgpuShaderModuleRelease)

/** The layout of one bind group a pipeline expects. */
actual class GpuBindGroupLayout internal constructor(handle: MemorySegment) : GpuHandle(handle, wgpu_h::wgpuBindGroupLayoutRelease)

/** Resources bound to a pipeline's group, made by [GpuDevice.createBindGroup]. */
actual class GpuBindGroup internal constructor(handle: MemorySegment) : GpuHandle(handle, wgpu_h::wgpuBindGroupRelease)

/** Recorded commands, ready for [GpuQueue.submit]. */
actual class GpuCommandBuffer internal constructor(handle: MemorySegment) : GpuHandle(handle, wgpu_h::wgpuCommandBufferRelease)

/** A render pipeline: shaders and fixed-function state, made by [GpuDevice.createRenderPipeline]. */
actual class GpuRenderPipeline internal constructor(handle: MemorySegment) : GpuHandle(handle, wgpu_h::wgpuRenderPipelineRelease) {
    /** The layout of bind group [index] as the shaders declare it. Close it once the bind group is made. */
    actual fun bindGroupLayout(index: Int): GpuBindGroupLayout = GpuBindGroupLayout(wgpu_h.wgpuRenderPipelineGetBindGroupLayout(handle, index))
}

/** A compute pipeline: a shader's compute entry point, made by [GpuDevice.createComputePipeline]. */
actual class GpuComputePipeline internal constructor(handle: MemorySegment) : GpuHandle(handle, wgpu_h::wgpuComputePipelineRelease) {
    /** The layout of bind group [index] as the shader declares it. Close it once the bind group is made. */
    actual fun bindGroupLayout(index: Int): GpuBindGroupLayout = GpuBindGroupLayout(wgpu_h.wgpuComputePipelineGetBindGroupLayout(handle, index))
}
