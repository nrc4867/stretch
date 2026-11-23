package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.webgpu.ffi.WGPUBufferMapCallback
import dev.chieppa.webgpu.ffi.WGPUBufferMapCallbackInfo
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/**
 * Starts mapping all of [buffer] for reading without waiting for the GPU. [onMapped] runs inside a later [pollEvents]
 * on this thread, with whether it worked and the error if not. The arena returned keeps the callback alive: close it
 * once the buffer is unmapped. Use from one thread.
 */
internal fun GpuDevice.mapReadAsync(buffer: GpuBuffer, onMapped: (ok: Boolean, message: String) -> Unit): Arena {
    val arena = Arena.ofConfined()
    val callback = WGPUBufferMapCallback.allocate({ result, text, _, _ ->
        onMapped(result == wgpu_h.WGPUMapAsyncStatus_Success(), readString(text))
    }, arena)
    val info = arena.allocate(WGPUBufferMapCallbackInfo.layout())
    WGPUBufferMapCallbackInfo.mode(info, wgpu_h.WGPUCallbackMode_AllowProcessEvents())
    WGPUBufferMapCallbackInfo.callback(info, callback)
    wgpu_h.wgpuBufferMapAsync(arena, buffer.handle, MAP_READ, 0, buffer.size, info)
    return arena
}

/** Lets finished GPU work report back, running map callbacks; with [wait], first blocks until all submitted work is done. */
internal fun GpuDevice.pollEvents(wait: Boolean) {
    wgpu_h.wgpuDevicePoll(handle, if (wait) 1 else 0, MemorySegment.NULL)
    wgpu_h.wgpuInstanceProcessEvents(instance)
}

/** The bytes of a buffer mapped for reading; valid until [unmap]. */
internal fun GpuBuffer.mappedBytes(): MemorySegment = wgpu_h.wgpuBufferGetConstMappedRange(handle, 0, size).reinterpret(size)

internal fun GpuBuffer.unmap() = wgpu_h.wgpuBufferUnmap(handle)
