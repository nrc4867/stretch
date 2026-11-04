@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.gpu.webgpu

import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Int32Array
import org.khronos.webgl.Int8Array
import org.khronos.webgl.set

private fun queueWrite(queue: JsAny, buffer: JsAny, offset: Double, data: ArrayBufferView): Unit =
    js("queue.writeBuffer(buffer, offset, data)")

private fun queueSubmit(queue: JsAny, commands: JsArray<JsAny>): Unit = js("queue.submit(commands)")

actual class GpuQueue internal constructor(handle: JsAny) : GpuJsHandle(handle) {
    actual fun writeBuffer(buffer: GpuBuffer, data: FloatArray, offset: Long, from: Int, count: Int) {
        if (count == 0) return
        val array = Float32Array(count)
        for (i in 0 until count) array[i] = data[from + i]
        queueWrite(handle, buffer.handle, offset.toDouble(), array)
    }

    actual fun writeBuffer(buffer: GpuBuffer, data: IntArray, offset: Long, from: Int, count: Int) {
        if (count == 0) return
        val array = Int32Array(count)
        for (i in 0 until count) array[i] = data[from + i]
        queueWrite(handle, buffer.handle, offset.toDouble(), array)
    }

    actual fun writeBuffer(buffer: GpuBuffer, data: ByteArray, offset: Long, from: Int, count: Int) {
        if (count == 0) return
        val array = Int8Array(count)
        for (i in 0 until count) array[i] = data[from + i]
        queueWrite(handle, buffer.handle, offset.toDouble(), array)
    }

    actual fun submit(vararg commands: GpuCommandBuffer) = queueSubmit(handle, commands.toList().toJs { it.handle })
}
