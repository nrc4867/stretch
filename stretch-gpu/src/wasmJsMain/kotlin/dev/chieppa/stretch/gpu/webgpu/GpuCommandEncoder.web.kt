@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.gpu.webgpu

private fun encoderBeginPass(encoder: JsAny, descriptor: JsAny): JsAny = js("encoder.beginRenderPass(descriptor)")

private fun encoderCopy(encoder: JsAny, source: JsAny, destination: JsAny, size: JsAny): Unit =
    js("encoder.copyTextureToBuffer(source, destination, size)")

private fun encoderBeginComputePass(encoder: JsAny, descriptor: JsAny): JsAny = js("encoder.beginComputePass(descriptor)")

private fun encoderCopyBuffer(encoder: JsAny, source: JsAny, sourceOffset: Double, destination: JsAny, destinationOffset: Double, size: Double): Unit =
    js("encoder.copyBufferToBuffer(source, sourceOffset, destination, destinationOffset, size)")

private fun encoderFinish(encoder: JsAny): JsAny = js("encoder.finish()")

actual class GpuCommandEncoder internal constructor(handle: JsAny) : GpuJsHandle(handle) {
    actual fun beginRenderPass(colors: List<ColorAttachment>, depth: DepthAttachment?, label: String?): GpuRenderPass =
        GpuRenderPass(
            encoderBeginPass(
                handle,
                jsObject {
                    this["label"] = label
                    this["colorAttachments"] = colors.toJs { color ->
                        jsObject {
                            this["view"] = color.view.handle
                            this["resolveTarget"] = color.resolveTarget?.handle
                            this["loadOp"] = color.load.webName
                            this["storeOp"] = color.store.webName
                            this["clearValue"] = jsObject {
                                this["r"] = color.clear.red.toDouble()
                                this["g"] = color.clear.green.toDouble()
                                this["b"] = color.clear.blue.toDouble()
                                this["a"] = color.clear.alpha.toDouble()
                            }
                        }
                    }
                    this["depthStencilAttachment"] = depth?.let {
                        jsObject {
                            this["view"] = it.view.handle
                            this["depthLoadOp"] = it.load.webName
                            this["depthStoreOp"] = it.store.webName
                            this["depthClearValue"] = it.clearDepth.toDouble()
                        }
                    }
                },
            ),
        )

    actual fun beginComputePass(label: String?): GpuComputePass =
        GpuComputePass(encoderBeginComputePass(handle, jsObject { this["label"] = label }))

    actual fun copyBufferToBuffer(source: GpuBuffer, destination: GpuBuffer, size: Long, sourceOffset: Long, destinationOffset: Long) =
        encoderCopyBuffer(handle, source.handle, sourceOffset.toDouble(), destination.handle, destinationOffset.toDouble(), size.toDouble())

    actual fun copyTextureToBuffer(
        texture: GpuTexture,
        buffer: GpuBuffer,
        bytesPerRow: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        bufferOffset: Long,
        aspect: TextureAspect,
    ) = encoderCopy(
        handle,
        jsObject {
            this["texture"] = texture.handle
            this["aspect"] = aspect.webName
            this["origin"] = jsObject {
                this["x"] = x
                this["y"] = y
            }
        },
        jsObject {
            this["buffer"] = buffer.handle
            this["offset"] = bufferOffset
            this["bytesPerRow"] = bytesPerRow
            this["rowsPerImage"] = height
        },
        jsObject {
            this["width"] = width
            this["height"] = height
        },
    )

    actual fun finish(): GpuCommandBuffer = GpuCommandBuffer(encoderFinish(handle))
}
