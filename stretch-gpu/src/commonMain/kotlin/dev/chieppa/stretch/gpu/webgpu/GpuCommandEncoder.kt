package dev.chieppa.stretch.gpu.webgpu

/** Records commands for one submission. [finish] turns them into a [GpuCommandBuffer]; the encoder is spent then. */
expect class GpuCommandEncoder : AutoCloseable {
    override fun close()

    fun beginRenderPass(colors: List<ColorAttachment>, depth: DepthAttachment? = null, label: String? = null): GpuRenderPass

    /**
     * Copies [texture], or the [width] by [height] texels from [x], [y], into [buffer] from [bufferOffset], each row
     * starting [bytesPerRow] bytes after the last: a multiple of 256, see [dev.chieppa.stretch.gpu.paddedRowBytes].
     * [aspect] picks the depth of a depth texture, which copies only on its own.
     */
    fun copyTextureToBuffer(
        texture: GpuTexture,
        buffer: GpuBuffer,
        bytesPerRow: Int,
        x: Int = 0,
        y: Int = 0,
        width: Int = texture.width,
        height: Int = texture.height,
        bufferOffset: Long = 0,
        aspect: TextureAspect = TextureAspect.All,
    )

    fun finish(): GpuCommandBuffer
}
