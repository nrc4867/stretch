package dev.chieppa.stretch.gpu.webgpu

/** What a buffer is used for; combine with `+`. Values are webgpu.h's WGPUBufferUsage bits. */
@JvmInline
value class BufferUsage internal constructor(internal val bits: Long) {
    operator fun plus(other: BufferUsage) = BufferUsage(bits or other.bits)

    companion object {
        val MapRead = BufferUsage(0x1)
        val MapWrite = BufferUsage(0x2)
        val CopySrc = BufferUsage(0x4)
        val CopyDst = BufferUsage(0x8)
        val Index = BufferUsage(0x10)
        val Vertex = BufferUsage(0x20)
        val Uniform = BufferUsage(0x40)
        val Storage = BufferUsage(0x80)
        val Indirect = BufferUsage(0x100)
    }
}

/** What a texture is used for; combine with `+`. Values are webgpu.h's WGPUTextureUsage bits. */
@JvmInline
value class TextureUsage internal constructor(internal val bits: Long) {
    operator fun plus(other: TextureUsage) = TextureUsage(bits or other.bits)

    companion object {
        val CopySrc = TextureUsage(0x1)
        val CopyDst = TextureUsage(0x2)
        val TextureBinding = TextureUsage(0x4)
        val StorageBinding = TextureUsage(0x8)
        val RenderAttachment = TextureUsage(0x10)
    }
}

/** webgpu.h's WGPUColorWriteMask_All: write every channel. */
internal const val COLOR_WRITE_ALL = 0xFL

/** webgpu.h's WGPUMapMode_Read. */
internal const val MAP_READ = 0x1L

/** webgpu.h's WGPU_WHOLE_SIZE: to the end of the buffer. */
internal const val WHOLE_SIZE = -1L

/** webgpu.h's WGPU_DEPTH_SLICE_UNDEFINED, for 2D color attachments. */
internal const val DEPTH_SLICE_UNDEFINED = -1
