package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.gpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment

/** A block of GPU memory, [size] bytes long. */
class GpuBuffer internal constructor(handle: MemorySegment, val size: Long) : GpuHandle(handle, wgpu_h::wgpuBufferRelease)

/** A texture, [width] by [height] pixels in [format]. Draw into it or sample it through a [GpuTextureView]. */
class GpuTexture internal constructor(handle: MemorySegment, val width: Int, val height: Int, val format: TextureFormat) :
    GpuHandle(handle, wgpu_h::wgpuTextureRelease) {
    /** A view of the whole texture. */
    fun createView(): GpuTextureView = GpuTextureView(wgpu_h.wgpuTextureCreateView(handle, MemorySegment.NULL))
}

/** A view of a texture, as render passes and bind groups take it. */
class GpuTextureView internal constructor(handle: MemorySegment) : GpuHandle(handle, wgpu_h::wgpuTextureViewRelease)

/** Compiled WGSL. */
class GpuShaderModule internal constructor(handle: MemorySegment) : GpuHandle(handle, wgpu_h::wgpuShaderModuleRelease)

/** The layout of one bind group a pipeline expects. */
class GpuBindGroupLayout internal constructor(handle: MemorySegment) : GpuHandle(handle, wgpu_h::wgpuBindGroupLayoutRelease)

/** Resources bound to a pipeline's group, made by [GpuDevice.createBindGroup]. */
class GpuBindGroup internal constructor(handle: MemorySegment) : GpuHandle(handle, wgpu_h::wgpuBindGroupRelease)

/** Recorded commands, ready for [GpuQueue.submit]. */
class GpuCommandBuffer internal constructor(handle: MemorySegment) : GpuHandle(handle, wgpu_h::wgpuCommandBufferRelease)

/** A render pipeline: shaders and fixed-function state, made by [GpuDevice.createRenderPipeline]. */
class GpuRenderPipeline internal constructor(handle: MemorySegment) : GpuHandle(handle, wgpu_h::wgpuRenderPipelineRelease) {
    /** The layout of bind group [index] as the shaders declare it. Close it once the bind group is made. */
    fun bindGroupLayout(index: Int): GpuBindGroupLayout = GpuBindGroupLayout(wgpu_h.wgpuRenderPipelineGetBindGroupLayout(handle, index))
}
