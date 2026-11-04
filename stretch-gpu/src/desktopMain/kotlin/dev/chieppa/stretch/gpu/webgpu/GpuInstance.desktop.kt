package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.frame.SurfaceHandle
import dev.chieppa.stretch.gpu.ffi.WGPUAdapterInfo
import dev.chieppa.stretch.gpu.ffi.WGPUDeviceDescriptor
import dev.chieppa.stretch.gpu.ffi.WGPUDeviceLostCallback
import dev.chieppa.stretch.gpu.ffi.WGPUDeviceLostCallbackInfo
import dev.chieppa.stretch.gpu.ffi.WGPUInstanceDescriptor
import dev.chieppa.stretch.gpu.ffi.WGPUInstanceExtras
import dev.chieppa.stretch.gpu.ffi.WGPURequestAdapterCallback
import dev.chieppa.stretch.gpu.ffi.WGPURequestAdapterCallbackInfo
import dev.chieppa.stretch.gpu.ffi.WGPURequestAdapterOptions
import dev.chieppa.stretch.gpu.ffi.WGPURequestDeviceCallback
import dev.chieppa.stretch.gpu.ffi.WGPURequestDeviceCallbackInfo
import dev.chieppa.stretch.gpu.ffi.WGPUSurfaceDescriptor
import dev.chieppa.stretch.gpu.ffi.WGPUSurfaceSourceWindowsHWND
import dev.chieppa.stretch.gpu.ffi.WGPUUncapturedErrorCallback
import dev.chieppa.stretch.gpu.ffi.WGPUUncapturedErrorCallbackInfo
import dev.chieppa.stretch.gpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/**
 * A WebGPU instance with the adapter and device it picked, and the surface it presents to when it has one. Owns
 * the callbacks wgpu keeps (device errors and loss), which live as long as it does.
 */
internal actual class GpuInstance private constructor(
    private val callbacks: Arena,
    private val instance: MemorySegment,
    private val adapter: MemorySegment,
    actual val surface: GpuSurface?,
    actual val device: GpuDevice,
    actual val description: String,
) : AutoCloseable {
    @Volatile private var lostReason: String? = null

    actual val lost: String? get() = lostReason

    actual fun surfaceFormats(): List<TextureFormat> = surface?.formats(adapter).orEmpty()

    actual override fun close() {
        device.close()
        surface?.close()
        wgpu_h.wgpuAdapterRelease(adapter)
        wgpu_h.wgpuInstanceRelease(instance)
        callbacks.close()
    }

    actual companion object {
        /** [onError] is called on whichever thread wgpu reports from. */
        actual suspend fun create(
            window: SurfaceHandle?,
            backend: GpuBackend?,
            power: PowerPreference,
            onError: (String) -> Unit,
        ): GpuInstance {
            WgpuLibrary.load()
            val callbacks = Arena.ofShared()
            return Arena.ofConfined().use { arena ->
                val instance = createInstance(arena, backend)
                val surface = window?.let { GpuSurface(createSurface(arena, instance, it)) }
                val adapter = requestAdapter(arena, instance, surface, power)
                val description = describe(arena, adapter)
                var gpu: GpuInstance? = null
                val device = requestDevice(arena, callbacks, instance, adapter, onError) { reason -> gpu?.lostReason = reason }
                GpuInstance(callbacks, instance, adapter, surface, GpuDevice(device, instance), description).also { gpu = it }
            }
        }

        private fun createInstance(arena: Arena, backend: GpuBackend?): MemorySegment {
            val extras = arena.allocate(WGPUInstanceExtras.layout())
            chain(extras, wgpu_h.WGPUSType_InstanceExtras())
            WGPUInstanceExtras.backends(extras, backend?.bits ?: 0)
            // Direct3D 12 compiles shaders with FXC, built into Windows. Left to itself wgpu takes the first DXC on the
            // PATH, and an old one fails every shader.
            WGPUInstanceExtras.dx12ShaderCompiler(extras, wgpu_h.WGPUDx12Compiler_Fxc())
            val descriptor = arena.allocate(WGPUInstanceDescriptor.layout())
            WGPUInstanceDescriptor.nextInChain(descriptor, extras)
            return wgpu_h.wgpuCreateInstance(descriptor).also { check(it != MemorySegment.NULL) { "WebGPU is not available" } }
        }

        private fun createSurface(arena: Arena, instance: MemorySegment, window: SurfaceHandle): MemorySegment {
            val source = when (window) {
                is SurfaceHandle.Win32 -> arena.allocate(WGPUSurfaceSourceWindowsHWND.layout()).also {
                    chain(it, wgpu_h.WGPUSType_SurfaceSourceWindowsHWND())
                    WGPUSurfaceSourceWindowsHWND.hinstance(it, MemorySegment.ofAddress(window.hinstance))
                    WGPUSurfaceSourceWindowsHWND.hwnd(it, MemorySegment.ofAddress(window.hwnd))
                }
            }
            val descriptor = arena.allocate(WGPUSurfaceDescriptor.layout())
            WGPUSurfaceDescriptor.nextInChain(descriptor, source)
            return wgpu_h.wgpuInstanceCreateSurface(instance, descriptor)
                .also { check(it != MemorySegment.NULL) { "WebGPU could not make a surface for the window" } }
        }

        private fun requestAdapter(arena: Arena, instance: MemorySegment, surface: GpuSurface?, power: PowerPreference): MemorySegment {
            val options = arena.allocate(WGPURequestAdapterOptions.layout())
            WGPURequestAdapterOptions.featureLevel(options, wgpu_h.WGPUFeatureLevel_Core())
            WGPURequestAdapterOptions.powerPreference(options, power.native)
            WGPURequestAdapterOptions.compatibleSurface(options, surface?.handle ?: MemorySegment.NULL)
            var result: MemorySegment? = null
            var failure: String? = null
            val info = arena.allocate(WGPURequestAdapterCallbackInfo.layout())
            WGPURequestAdapterCallbackInfo.mode(info, wgpu_h.WGPUCallbackMode_AllowProcessEvents())
            WGPURequestAdapterCallbackInfo.callback(
                info,
                WGPURequestAdapterCallback.allocate({ status, adapter, message, _, _ ->
                    // A pointer passed to a callback is only valid during it, so keep its address instead.
                    if (status == wgpu_h.WGPURequestAdapterStatus_Success()) result = MemorySegment.ofAddress(adapter.address()) else failure = readString(message).ifBlank { "status $status" }
                }, arena),
            )
            wgpu_h.wgpuInstanceRequestAdapter(arena, instance, options, info)
            while (result == null && failure == null) wgpu_h.wgpuInstanceProcessEvents(instance)
            return result ?: error("No GPU adapter suits WebGPU: $failure")
        }

        private fun requestDevice(
            arena: Arena,
            callbacks: Arena,
            instance: MemorySegment,
            adapter: MemorySegment,
            onError: (String) -> Unit,
            onLost: (String) -> Unit,
        ): MemorySegment {
            val descriptor = arena.allocate(WGPUDeviceDescriptor.layout())
            val errors = WGPUDeviceDescriptor.uncapturedErrorCallbackInfo(descriptor)
            WGPUUncapturedErrorCallbackInfo.callback(
                errors,
                WGPUUncapturedErrorCallback.allocate({ _, _, message, _, _ -> onError(readString(message)) }, callbacks),
            )
            val loss = WGPUDeviceDescriptor.deviceLostCallbackInfo(descriptor)
            WGPUDeviceLostCallbackInfo.mode(loss, wgpu_h.WGPUCallbackMode_AllowSpontaneous())
            WGPUDeviceLostCallbackInfo.callback(
                loss,
                WGPUDeviceLostCallback.allocate({ _, reason, message, _, _ ->
                    // Released on purpose, or never made: neither is a loss to report.
                    if (reason != wgpu_h.WGPUDeviceLostReason_Destroyed() && reason != wgpu_h.WGPUDeviceLostReason_CallbackCancelled()) {
                        val text = "The GPU device was lost: ${readString(message)}"
                        onLost(text)
                        onError(text)
                    }
                }, callbacks),
            )
            var result: MemorySegment? = null
            var failure: String? = null
            val info = arena.allocate(WGPURequestDeviceCallbackInfo.layout())
            WGPURequestDeviceCallbackInfo.mode(info, wgpu_h.WGPUCallbackMode_AllowProcessEvents())
            WGPURequestDeviceCallbackInfo.callback(
                info,
                WGPURequestDeviceCallback.allocate({ status, device, message, _, _ ->
                    if (status == wgpu_h.WGPURequestDeviceStatus_Success()) result = MemorySegment.ofAddress(device.address()) else failure = readString(message).ifBlank { "status $status" }
                }, arena),
            )
            wgpu_h.wgpuAdapterRequestDevice(arena, adapter, descriptor, info)
            while (result == null && failure == null) wgpu_h.wgpuInstanceProcessEvents(instance)
            return result ?: error("The GPU device could not be made: $failure")
        }

        /** The adapter's name and backend, such as "Intel(R) Arc(TM) 140V GPU (Vulkan)". */
        private fun describe(arena: Arena, adapter: MemorySegment): String {
            val info = arena.allocate(WGPUAdapterInfo.layout())
            wgpu_h.wgpuAdapterGetInfo(adapter, info)
            // wgpu puts the driver version in the description, so the device name comes first.
            val name = listOf(WGPUAdapterInfo.device(info), WGPUAdapterInfo.description(info), WGPUAdapterInfo.vendor(info))
                .map(::readString).firstOrNull { it.isNotBlank() } ?: "Unknown GPU"
            val backend = when (WGPUAdapterInfo.backendType(info)) {
                wgpu_h.WGPUBackendType_Vulkan() -> "Vulkan"
                wgpu_h.WGPUBackendType_D3D12() -> "Direct3D 12"
                wgpu_h.WGPUBackendType_Metal() -> "Metal"
                wgpu_h.WGPUBackendType_OpenGL(), wgpu_h.WGPUBackendType_OpenGLES() -> "OpenGL"
                else -> "other backend"
            }
            wgpu_h.wgpuAdapterInfoFreeMembers(info)
            return "$name ($backend)"
        }
    }
}
