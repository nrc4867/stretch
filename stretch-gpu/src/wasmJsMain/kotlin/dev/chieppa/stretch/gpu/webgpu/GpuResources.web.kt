@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.gpu.webgpu

private fun destroyBuffer(buffer: JsAny): Unit = js("buffer.destroy()")

private fun destroyTexture(texture: JsAny): Unit = js("texture.destroy()")

private fun textureView(texture: JsAny): JsAny = js("texture.createView()")

private fun pipelineLayout(pipeline: JsAny, index: Int): JsAny = js("pipeline.getBindGroupLayout(index)")

/** A WebGPU object in JavaScript; the browser's garbage collector frees those with no [close] of their own. */
abstract class GpuJsHandle internal constructor(internal val handle: JsAny) : AutoCloseable {
    override fun close() = Unit
}

actual class GpuBuffer internal constructor(handle: JsAny, actual val size: Long) : GpuJsHandle(handle) {
    actual override fun close() = destroyBuffer(handle)
}

/** With [owned] false the texture belongs to a canvas, which destroys it itself. */
actual class GpuTexture internal constructor(
    handle: JsAny,
    actual val width: Int,
    actual val height: Int,
    actual val format: TextureFormat,
    private val owned: Boolean = true,
) : GpuJsHandle(handle) {
    actual fun createView(): GpuTextureView = GpuTextureView(textureView(handle))

    actual override fun close() {
        if (owned) destroyTexture(handle)
    }
}

actual class GpuTextureView internal constructor(handle: JsAny) : GpuJsHandle(handle)

actual class GpuShaderModule internal constructor(handle: JsAny) : GpuJsHandle(handle)

actual class GpuBindGroupLayout internal constructor(handle: JsAny) : GpuJsHandle(handle)

actual class GpuBindGroup internal constructor(handle: JsAny) : GpuJsHandle(handle)

actual class GpuCommandBuffer internal constructor(handle: JsAny) : GpuJsHandle(handle)

actual class GpuRenderPipeline internal constructor(handle: JsAny) : GpuJsHandle(handle) {
    actual fun bindGroupLayout(index: Int): GpuBindGroupLayout = GpuBindGroupLayout(pipelineLayout(handle, index))
}

actual class GpuComputePipeline internal constructor(handle: JsAny) : GpuJsHandle(handle) {
    actual fun bindGroupLayout(index: Int): GpuBindGroupLayout = GpuBindGroupLayout(pipelineLayout(handle, index))
}
