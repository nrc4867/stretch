package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.gpu.ffi.WGPUSurfaceCapabilities
import dev.chieppa.stretch.gpu.ffi.WGPUSurfaceConfiguration
import dev.chieppa.stretch.gpu.ffi.WGPUSurfaceTexture
import dev.chieppa.stretch.gpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** A window's surface that frames are presented to. */
internal class GpuSurface(handle: MemorySegment) : GpuHandle(handle, wgpu_h::wgpuSurfaceRelease) {
    private var width = 0
    private var height = 0
    private var format = TextureFormat.BGRA8Unorm

    /** The texture formats [adapter] can present to this surface with, the ones Stretch names. */
    fun formats(adapter: MemorySegment): List<TextureFormat> = Arena.ofConfined().use { arena ->
        val capabilities = arena.allocate(WGPUSurfaceCapabilities.layout())
        wgpu_h.wgpuSurfaceGetCapabilities(handle, adapter, capabilities)
        val count = WGPUSurfaceCapabilities.formatCount(capabilities)
        val values = WGPUSurfaceCapabilities.formats(capabilities).reinterpret(count * Int.SIZE_BYTES)
        val formats = (0 until count).mapNotNull { TextureFormat.of(values.getAtIndex(ValueLayout.JAVA_INT, it)) }
        wgpu_h.wgpuSurfaceCapabilitiesFreeMembers(capabilities)
        formats
    }

    fun configure(device: GpuDevice, format: TextureFormat, width: Int, height: Int, presentMode: PresentMode) = Arena.ofConfined().use { arena ->
        val configuration = arena.allocate(WGPUSurfaceConfiguration.layout())
        WGPUSurfaceConfiguration.device(configuration, device.handle)
        WGPUSurfaceConfiguration.format(configuration, format.native)
        WGPUSurfaceConfiguration.usage(configuration, TextureUsage.RenderAttachment.bits)
        WGPUSurfaceConfiguration.width(configuration, width)
        WGPUSurfaceConfiguration.height(configuration, height)
        WGPUSurfaceConfiguration.alphaMode(configuration, wgpu_h.WGPUCompositeAlphaMode_Opaque())
        WGPUSurfaceConfiguration.presentMode(configuration, presentMode.native)
        wgpu_h.wgpuSurfaceConfigure(handle, configuration)
        this.width = width
        this.height = height
        this.format = format
    }

    /**
     * The texture to draw the next frame into, or null when the surface has none for now (the window is hidden or
     * resizing, or the surface went out of date) and should be configured again. Throws on a lasting error.
     */
    fun acquire(): GpuTexture? = Arena.ofConfined().use { arena ->
        val current = arena.allocate(WGPUSurfaceTexture.layout())
        wgpu_h.wgpuSurfaceGetCurrentTexture(handle, current)
        val texture = WGPUSurfaceTexture.texture(current)
        when (WGPUSurfaceTexture.status(current)) {
            wgpu_h.WGPUSurfaceGetCurrentTextureStatus_SuccessOptimal(),
            wgpu_h.WGPUSurfaceGetCurrentTextureStatus_SuccessSuboptimal(),
            -> GpuTexture(texture, width, height, format)
            wgpu_h.WGPUSurfaceGetCurrentTextureStatus_Error() -> error("The GPU surface failed; it may not be configured")
            else -> {
                if (texture != MemorySegment.NULL) wgpu_h.wgpuTextureRelease(texture)
                null
            }
        }
    }

    /** Shows the acquired texture; false when the surface could not, and should be configured again. */
    fun present(): Boolean = wgpu_h.wgpuSurfacePresent(handle) == wgpu_h.WGPUStatus_Success()
}
