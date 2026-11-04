package dev.chieppa.stretch.gpu.webgpu

/** The device's queue: uploads data and runs recorded commands, in order. */
expect class GpuQueue : AutoCloseable {
    override fun close()

    /** Writes [count] floats of [data] from [from] into [buffer] at byte [offset], before the next submission runs. */
    fun writeBuffer(buffer: GpuBuffer, data: FloatArray, offset: Long = 0, from: Int = 0, count: Int = data.size - from)

    /** Writes [count] ints of [data] from [from] into [buffer] at byte [offset], before the next submission runs. */
    fun writeBuffer(buffer: GpuBuffer, data: IntArray, offset: Long = 0, from: Int = 0, count: Int = data.size - from)

    /** Writes [count] bytes of [data] from [from] into [buffer] at byte [offset], before the next submission runs. */
    fun writeBuffer(buffer: GpuBuffer, data: ByteArray, offset: Long = 0, from: Int = 0, count: Int = data.size - from)

    /** Runs [commands] in order and releases them. */
    fun submit(vararg commands: GpuCommandBuffer)
}
