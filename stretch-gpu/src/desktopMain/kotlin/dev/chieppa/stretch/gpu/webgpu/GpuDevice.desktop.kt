package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.gpu.ffi.WGPUBindGroupDescriptor
import dev.chieppa.stretch.gpu.ffi.WGPUBindGroupEntry
import dev.chieppa.stretch.gpu.ffi.WGPUBufferDescriptor
import dev.chieppa.stretch.gpu.ffi.WGPUBufferMapCallback
import dev.chieppa.stretch.gpu.ffi.WGPUBufferMapCallbackInfo
import dev.chieppa.stretch.gpu.ffi.WGPUExtent3D
import dev.chieppa.stretch.gpu.ffi.WGPUShaderModuleDescriptor
import dev.chieppa.stretch.gpu.ffi.WGPUShaderSourceWGSL
import dev.chieppa.stretch.gpu.ffi.WGPUTextureDescriptor
import dev.chieppa.stretch.gpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * A WebGPU device: makes buffers, textures, shaders, pipelines and bind groups, and owns the [queue] that runs
 * commands. Made by a [dev.chieppa.stretch.gpu.GpuContext]'s session; use it from one thread at a time.
 */
actual class GpuDevice internal constructor(handle: MemorySegment, private val instance: MemorySegment) :
    GpuHandle(handle, wgpu_h::wgpuDeviceRelease) {
    actual val queue: GpuQueue = GpuQueue(wgpu_h.wgpuDeviceGetQueue(handle))

    actual fun createBuffer(size: Long, usage: BufferUsage, label: String?): GpuBuffer = Arena.ofConfined().use { arena ->
        val descriptor = arena.allocate(WGPUBufferDescriptor.layout())
        arena.setString(WGPUBufferDescriptor.label(descriptor), label)
        WGPUBufferDescriptor.usage(descriptor, usage.bits)
        WGPUBufferDescriptor.size(descriptor, size)
        GpuBuffer(wgpu_h.wgpuDeviceCreateBuffer(handle, descriptor), size)
    }

    actual fun createTexture(spec: TextureSpec): GpuTexture = Arena.ofConfined().use { arena ->
        val descriptor = arena.allocate(WGPUTextureDescriptor.layout())
        arena.setString(WGPUTextureDescriptor.label(descriptor), spec.label)
        WGPUTextureDescriptor.usage(descriptor, spec.usage.bits)
        WGPUTextureDescriptor.dimension(descriptor, wgpu_h.WGPUTextureDimension_2D())
        val size = WGPUTextureDescriptor.size(descriptor)
        WGPUExtent3D.width(size, spec.width)
        WGPUExtent3D.height(size, spec.height)
        WGPUExtent3D.depthOrArrayLayers(size, 1)
        WGPUTextureDescriptor.format(descriptor, spec.format.native)
        WGPUTextureDescriptor.mipLevelCount(descriptor, 1)
        WGPUTextureDescriptor.sampleCount(descriptor, spec.sampleCount)
        GpuTexture(wgpu_h.wgpuDeviceCreateTexture(handle, descriptor), spec.width, spec.height, spec.format)
    }

    /** Compiles [wgsl]. Mistakes in it show up as errors through the context's error callback. */
    actual fun createShaderModule(wgsl: String, label: String?): GpuShaderModule = Arena.ofConfined().use { arena ->
        val source = arena.allocate(WGPUShaderSourceWGSL.layout())
        chain(source, wgpu_h.WGPUSType_ShaderSourceWGSL())
        arena.setString(WGPUShaderSourceWGSL.code(source), wgsl)
        val descriptor = arena.allocate(WGPUShaderModuleDescriptor.layout())
        WGPUShaderModuleDescriptor.nextInChain(descriptor, source)
        arena.setString(WGPUShaderModuleDescriptor.label(descriptor), label)
        GpuShaderModule(wgpu_h.wgpuDeviceCreateShaderModule(handle, descriptor))
    }

    actual fun createRenderPipeline(spec: RenderPipelineSpec): GpuRenderPipeline = Arena.ofConfined().use { arena ->
        GpuRenderPipeline(wgpu_h.wgpuDeviceCreateRenderPipeline(handle, renderPipelineDescriptor(arena, spec)))
    }

    /** Binds [buffers] to the group with [layout]. */
    actual fun createBindGroup(layout: GpuBindGroupLayout, buffers: List<BufferBinding>, label: String?): GpuBindGroup =
        Arena.ofConfined().use { arena ->
            val descriptor = arena.allocate(WGPUBindGroupDescriptor.layout())
            arena.setString(WGPUBindGroupDescriptor.label(descriptor), label)
            WGPUBindGroupDescriptor.layout(descriptor, layout.handle)
            WGPUBindGroupDescriptor.entryCount(descriptor, buffers.size.toLong())
            WGPUBindGroupDescriptor.entries(
                descriptor,
                arena.structs(buffers.size, WGPUBindGroupEntry.layout()) { i, entry ->
                    val binding = buffers[i]
                    WGPUBindGroupEntry.binding(entry, binding.binding)
                    WGPUBindGroupEntry.buffer(entry, binding.buffer.handle)
                    WGPUBindGroupEntry.offset(entry, binding.offset)
                    WGPUBindGroupEntry.size(entry, binding.size ?: WHOLE_SIZE)
                },
            )
            GpuBindGroup(wgpu_h.wgpuDeviceCreateBindGroup(handle, descriptor))
        }

    actual fun createCommandEncoder(): GpuCommandEncoder = GpuCommandEncoder(wgpu_h.wgpuDeviceCreateCommandEncoder(handle, MemorySegment.NULL))

    /**
     * Blocks the calling thread until the GPU has finished.
     */
    actual suspend fun readBuffer(buffer: GpuBuffer, size: Long): ByteArray = Arena.ofConfined().use { arena ->
        var status: Int? = null
        var message = ""
        val callback = WGPUBufferMapCallback.allocate({ result, text, _, _ ->
            status = result
            message = readString(text)
        }, arena)
        val info = arena.allocate(WGPUBufferMapCallbackInfo.layout())
        WGPUBufferMapCallbackInfo.mode(info, wgpu_h.WGPUCallbackMode_AllowProcessEvents())
        WGPUBufferMapCallbackInfo.callback(info, callback)
        wgpu_h.wgpuBufferMapAsync(arena, buffer.handle, MAP_READ, 0, size, info)
        while (status == null) {
            wgpu_h.wgpuDevicePoll(handle, 1, MemorySegment.NULL)
            wgpu_h.wgpuInstanceProcessEvents(instance)
        }
        check(status == wgpu_h.WGPUMapAsyncStatus_Success()) { "Reading a GPU buffer failed: $message" }
        val mapped = wgpu_h.wgpuBufferGetConstMappedRange(buffer.handle, 0, size).reinterpret(size)
        val bytes = mapped.toArray(ValueLayout.JAVA_BYTE)
        wgpu_h.wgpuBufferUnmap(buffer.handle)
        bytes
    }

    actual override fun close() {
        queue.close()
        super.close()
    }
}
