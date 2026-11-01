package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.gpu.ffi.wgpu_h

/** Texture formats Stretch draws with. [native] is webgpu.h's WGPUTextureFormat value. */
enum class TextureFormat(private val value: () -> Int) {
    BGRA8Unorm(wgpu_h::WGPUTextureFormat_BGRA8Unorm),
    RGBA8Unorm(wgpu_h::WGPUTextureFormat_RGBA8Unorm),
    BGRA8UnormSrgb(wgpu_h::WGPUTextureFormat_BGRA8UnormSrgb),
    RGBA8UnormSrgb(wgpu_h::WGPUTextureFormat_RGBA8UnormSrgb),
    RGBA16Float(wgpu_h::WGPUTextureFormat_RGBA16Float),
    R32Uint(wgpu_h::WGPUTextureFormat_R32Uint),
    Depth24Plus(wgpu_h::WGPUTextureFormat_Depth24Plus),
    Depth32Float(wgpu_h::WGPUTextureFormat_Depth32Float),
    ;

    internal val native: Int get() = value()

    companion object {
        /** The format with webgpu.h value [value], or null for one Stretch does not name. */
        internal fun of(value: Int): TextureFormat? = entries.firstOrNull { it.native == value }
    }
}

/** The type of one vertex attribute. [native] is webgpu.h's WGPUVertexFormat value. */
enum class VertexFormat(private val value: () -> Int) {
    Float32(wgpu_h::WGPUVertexFormat_Float32),
    Float32x2(wgpu_h::WGPUVertexFormat_Float32x2),
    Float32x3(wgpu_h::WGPUVertexFormat_Float32x3),
    Float32x4(wgpu_h::WGPUVertexFormat_Float32x4),
    Uint32(wgpu_h::WGPUVertexFormat_Uint32),
    Uint32x4(wgpu_h::WGPUVertexFormat_Uint32x4),
    Unorm8x4(wgpu_h::WGPUVertexFormat_Unorm8x4),
    ;

    internal val native: Int get() = value()
}

/** The type of an index buffer's entries. */
enum class IndexFormat(private val value: () -> Int) {
    Uint16(wgpu_h::WGPUIndexFormat_Uint16),
    Uint32(wgpu_h::WGPUIndexFormat_Uint32),
    ;

    internal val native: Int get() = value()
}
