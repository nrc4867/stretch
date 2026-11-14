@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.gpu.webgpu

private fun passSetPipeline(pass: JsAny, pipeline: JsAny): Unit = js("pass.setPipeline(pipeline)")

private fun passSetBindGroup(pass: JsAny, index: Int, group: JsAny): Unit = js("pass.setBindGroup(index, group)")

private fun passDispatch(pass: JsAny, x: Int, y: Int, z: Int): Unit = js("pass.dispatchWorkgroups(x, y, z)")

private fun passDispatchIndirect(pass: JsAny, buffer: JsAny, offset: Double): Unit = js("pass.dispatchWorkgroupsIndirect(buffer, offset)")

private fun passEnd(pass: JsAny): Unit = js("pass.end()")

actual class GpuComputePass internal constructor(private val handle: JsAny) {
    actual fun setPipeline(pipeline: GpuComputePipeline) = passSetPipeline(handle, pipeline.handle)

    actual fun setBindGroup(index: Int, group: GpuBindGroup) = passSetBindGroup(handle, index, group.handle)

    actual fun dispatchWorkgroups(x: Int, y: Int, z: Int) = passDispatch(handle, x, y, z)

    actual fun dispatchWorkgroupsIndirect(buffer: GpuBuffer, offset: Long) = passDispatchIndirect(handle, buffer.handle, offset.toDouble())

    actual fun end() = passEnd(handle)
}
