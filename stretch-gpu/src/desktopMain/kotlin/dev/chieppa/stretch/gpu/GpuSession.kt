package dev.chieppa.stretch.gpu

import dev.chieppa.stretch.frame.SurfaceHandle
import dev.chieppa.stretch.frame.SurfaceSize
import ffi.JvmNativeAddress
import ffi.LibraryLoader
import io.ygdrasil.webgpu.Adapter
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.DeviceDescriptor
import io.ygdrasil.webgpu.GPUDevice
import io.ygdrasil.webgpu.GPUTextureFormat
import io.ygdrasil.webgpu.GPUUncapturedErrorCallback
import io.ygdrasil.webgpu.NativeSurface
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.WGPU
import kotlin.jvm.internal.DefaultConstructorMarker
import kotlinx.coroutines.runBlocking
import java.lang.foreign.MemorySegment

/** The color formats a frame prefers, in order: plain 8-bit, so colors match Compose's. */
private val PREFERRED_FORMATS = listOf(GPUTextureFormat.BGRA8Unorm, GPUTextureFormat.RGBA8Unorm)

/**
 * A WebGPU instance, adapter and device, and the surface it presents to when it has one. Created and used on one
 * thread: a surface's render thread, or the caller's for offscreen work.
 */
internal class GpuSession private constructor(
    private val wgpu: WGPU,
    val surface: NativeSurface?,
    private val adapter: Adapter,
    val device: GPUDevice,
    val format: GPUTextureFormat,
    val description: String,
    private val options: GpuOptions,
) : AutoCloseable {
    private var configured = SurfaceSize.Zero

    /** Configures the surface for [size] if it changed, or always when [force]d after the surface went out of date. */
    fun configure(size: SurfaceSize, force: Boolean = false) {
        val surface = surface ?: return
        if (!force && size.width == configured.width && size.height == configured.height) return
        surface.configure(
            SurfaceConfiguration(device = device, format = format, alphaMode = CompositeAlphaMode.Opaque, presentMode = options.presentMode),
            size.width.toUInt(),
            size.height.toUInt(),
        )
        configured = size
    }

    override fun close() {
        device.close()
        adapter.close()
        surface?.close()
        wgpu.close()
    }

    companion object {
        /** A session presenting to [handle]'s window. */
        fun forSurface(handle: SurfaceHandle, options: GpuOptions): GpuSession = create(options) { wgpu ->
            when (handle) {
                is SurfaceHandle.Win32 -> wgpu.getSurfaceFromWindows(address(handle.hinstance), address(handle.hwnd))
                    ?: error("WebGPU could not make a surface for the window")
            }
        }

        /** A session without a surface, for rendering into textures. */
        fun offscreen(options: GpuOptions): GpuSession = create(options) { null }

        private fun address(value: Long) = JvmNativeAddress(MemorySegment.ofAddress(value))

        /**
         * A null surface, for asking for an adapter without one: wgpu4k only reads the surface's handle as the
         * adapter's compatible surface, where null means any. Its constructor is internal, so it is made reflectively.
         * Never configured or closed.
         */
        private fun noSurface(): NativeSurface = NativeSurface::class.java
            .getDeclaredConstructor(JvmNativeAddress::class.java, DefaultConstructorMarker::class.java)
            .newInstance(JvmNativeAddress(MemorySegment.NULL), null)

        private fun create(options: GpuOptions, surfaceFor: (WGPU) -> NativeSurface?): GpuSession = runBlocking {
            LibraryLoader.load()
            val wgpu = WGPU.createInstance(options.backend) ?: error("WebGPU is not available")
            val surface = surfaceFor(wgpu)
            val adapter = wgpu.requestAdapter(surface ?: noSurface(), options.powerPreference) ?: error("No GPU adapter suits WebGPU")
            val device = adapter.requestDevice(
                DeviceDescriptor(onUncapturedError = GPUUncapturedErrorCallback { options.onError(it.message) }),
            ).getOrThrow()
            val format = if (surface == null) {
                GPUTextureFormat.RGBA8Unorm
            } else {
                surface.computeSurfaceCapabilities(adapter)
                val supported = surface.supportedFormats
                PREFERRED_FORMATS.firstOrNull { it in supported } ?: supported.first()
            }
            val info = adapter.info
            val name = info.description.ifBlank { info.device }
            GpuSession(wgpu, surface, adapter, device, format, "$name (${options.backend ?: "automatic backend"})", options)
        }
    }
}
