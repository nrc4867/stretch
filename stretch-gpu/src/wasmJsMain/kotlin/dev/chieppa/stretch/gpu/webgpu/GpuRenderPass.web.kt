@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.gpu.webgpu

private fun passSetPipeline(pass: JsAny, pipeline: JsAny): Unit = js("pass.setPipeline(pipeline)")

private fun passSetBindGroup(pass: JsAny, index: Int, group: JsAny): Unit = js("pass.setBindGroup(index, group)")

private fun passSetVertexBuffer(pass: JsAny, slot: Int, buffer: JsAny, offset: Double, size: Double): Unit =
    js("pass.setVertexBuffer(slot, buffer, offset, size)")

private fun passSetIndexBuffer(pass: JsAny, buffer: JsAny, format: String, offset: Double, size: Double): Unit =
    js("pass.setIndexBuffer(buffer, format, offset, size)")

private fun passSetScissor(pass: JsAny, x: Int, y: Int, width: Int, height: Int): Unit = js("pass.setScissorRect(x, y, width, height)")

private fun passSetViewport(pass: JsAny, x: Float, y: Float, width: Float, height: Float, minDepth: Float, maxDepth: Float): Unit =
    js("pass.setViewport(x, y, width, height, minDepth, maxDepth)")

private fun passDraw(pass: JsAny, vertices: Int, instances: Int, firstVertex: Int, firstInstance: Int): Unit =
    js("pass.draw(vertices, instances, firstVertex, firstInstance)")

private fun passDrawIndexed(pass: JsAny, indices: Int, instances: Int, firstIndex: Int, baseVertex: Int, firstInstance: Int): Unit =
    js("pass.drawIndexed(indices, instances, firstIndex, baseVertex, firstInstance)")

private fun passEnd(pass: JsAny): Unit = js("pass.end()")

actual class GpuRenderPass internal constructor(private val handle: JsAny) {
    actual fun setPipeline(pipeline: GpuRenderPipeline) = passSetPipeline(handle, pipeline.handle)

    actual fun setBindGroup(index: Int, group: GpuBindGroup) = passSetBindGroup(handle, index, group.handle)

    actual fun setVertexBuffer(slot: Int, buffer: GpuBuffer, offset: Long, size: Long?) =
        passSetVertexBuffer(handle, slot, buffer.handle, offset.toDouble(), (size ?: (buffer.size - offset)).toDouble())

    actual fun setIndexBuffer(buffer: GpuBuffer, format: IndexFormat, offset: Long, size: Long?) =
        passSetIndexBuffer(handle, buffer.handle, format.webName, offset.toDouble(), (size ?: (buffer.size - offset)).toDouble())

    actual fun setScissorRect(x: Int, y: Int, width: Int, height: Int) = passSetScissor(handle, x, y, width, height)

    actual fun setViewport(x: Float, y: Float, width: Float, height: Float, minDepth: Float, maxDepth: Float) =
        passSetViewport(handle, x, y, width, height, minDepth, maxDepth)

    actual fun draw(vertexCount: Int, instanceCount: Int, firstVertex: Int, firstInstance: Int) =
        passDraw(handle, vertexCount, instanceCount, firstVertex, firstInstance)

    actual fun drawIndexed(indexCount: Int, instanceCount: Int, firstIndex: Int, baseVertex: Int, firstInstance: Int) =
        passDrawIndexed(handle, indexCount, instanceCount, firstIndex, baseVertex, firstInstance)

    actual fun end() = passEnd(handle)
}
