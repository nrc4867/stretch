package dev.chieppa.stretch.gpu.webgpu

/** Records commands for one submission. [finish] turns them into a [GpuCommandBuffer]; the encoder is spent then. */
expect class GpuCommandEncoder : AutoCloseable {
    override fun close()

    fun beginRenderPass(colors: List<ColorAttachment>, depth: DepthAttachment? = null, label: String? = null): GpuRenderPass

    fun beginComputePass(label: String? = null): GpuComputePass

    /**
     * Copies [size] bytes of [source] from [sourceOffset] into [destination] at [destinationOffset]: a compute pass's
     * results into a [BufferUsage.MapRead] buffer for [GpuDevice.readBuffer], say. The source needs
     * [BufferUsage.CopySrc] and the destination [BufferUsage.CopyDst]; sizes and offsets are multiples of 4.
     */
    fun copyBufferToBuffer(
        source: GpuBuffer,
        destination: GpuBuffer,
        size: Long = source.size,
        sourceOffset: Long = 0,
        destinationOffset: Long = 0,
    )

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
