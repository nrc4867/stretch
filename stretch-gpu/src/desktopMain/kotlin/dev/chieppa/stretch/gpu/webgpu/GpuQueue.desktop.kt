package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.gpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** The device's queue: uploads data and runs recorded commands, in order. */
actual class GpuQueue internal constructor(handle: MemorySegment) : GpuHandle(handle, wgpu_h::wgpuQueueRelease) {
    /** Writes [count] floats of [data] from [from] into [buffer] at byte [offset], before the next submission runs. */
    actual fun writeBuffer(buffer: GpuBuffer, data: FloatArray, offset: Long, from: Int, count: Int) =
        write(buffer, offset, count.toLong() * Float.SIZE_BYTES) { MemorySegment.copy(data, from, it, ValueLayout.JAVA_FLOAT, 0, count) }

    /** Writes [count] ints of [data] from [from] into [buffer] at byte [offset], before the next submission runs. */
    actual fun writeBuffer(buffer: GpuBuffer, data: IntArray, offset: Long, from: Int, count: Int) =
        write(buffer, offset, count.toLong() * Int.SIZE_BYTES) { MemorySegment.copy(data, from, it, ValueLayout.JAVA_INT, 0, count) }

    /** Writes [count] bytes of [data] from [from] into [buffer] at byte [offset], before the next submission runs. */
    actual fun writeBuffer(buffer: GpuBuffer, data: ByteArray, offset: Long, from: Int, count: Int) =
        write(buffer, offset, count.toLong()) { MemorySegment.copy(data, from, it, ValueLayout.JAVA_BYTE, 0, count) }

    private inline fun write(buffer: GpuBuffer, offset: Long, bytes: Long, fill: (MemorySegment) -> Unit) {
        if (bytes == 0L) return
        Arena.ofConfined().use { arena ->
            val staging = arena.allocate(bytes, Long.SIZE_BYTES.toLong())
            fill(staging)
            wgpu_h.wgpuQueueWriteBuffer(handle, buffer.handle, offset, staging, bytes)
        }
    }

    /** Runs [commands] in order and releases them. */
    actual fun submit(vararg commands: GpuCommandBuffer) {
        Arena.ofConfined().use { arena ->
            val list = arena.allocate(ValueLayout.ADDRESS, commands.size.toLong())
            commands.forEachIndexed { i, command -> list.setAtIndex(ValueLayout.ADDRESS, i.toLong(), command.handle) }
            wgpu_h.wgpuQueueSubmit(handle, commands.size.toLong(), list)
        }
        commands.forEach { it.close() }
    }
}
