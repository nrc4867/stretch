package dev.chieppa.stretch.gpu.webgpu

import kotlin.jvm.JvmInline

/** What a buffer is used for; combine with `+`. Values are WebGPU's GPUBufferUsage bits. */
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

/** What a texture is used for; combine with `+`. Values are WebGPU's GPUTextureUsage bits. */
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
