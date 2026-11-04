@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.gpu.webgpu

import kotlinx.coroutines.await
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import kotlin.js.Promise

private fun deviceQueue(device: JsAny): JsAny = js("device.queue")

private fun deviceCreateBuffer(device: JsAny, descriptor: JsAny): JsAny = js("device.createBuffer(descriptor)")

private fun deviceCreateTexture(device: JsAny, descriptor: JsAny): JsAny = js("device.createTexture(descriptor)")

private fun deviceCreateShader(device: JsAny, descriptor: JsAny): JsAny = js("device.createShaderModule(descriptor)")

private fun deviceCreatePipeline(device: JsAny, descriptor: JsAny): JsAny = js("device.createRenderPipeline(descriptor)")

private fun deviceCreateBindGroup(device: JsAny, descriptor: JsAny): JsAny = js("device.createBindGroup(descriptor)")

private fun deviceCreateEncoder(device: JsAny): JsAny = js("device.createCommandEncoder()")

private fun deviceDestroy(device: JsAny): Unit = js("device.destroy()")

/** A copy of the buffer's first [size] bytes, the buffer unmapped again. */
private fun readMapped(buffer: JsAny, mode: Int, size: Double): Promise<Uint8Array> = js("""buffer.mapAsync(mode, 0, size).then(() => {
    const bytes = new Uint8Array(buffer.getMappedRange(0, size).slice(0));
    buffer.unmap();
    return bytes;
})""")

/** GPUMapMode.READ. */
private const val MAP_READ = 1

private const val AUTO_LAYOUT = "auto"

actual class GpuDevice internal constructor(handle: JsAny) : GpuJsHandle(handle) {
    actual val queue: GpuQueue = GpuQueue(deviceQueue(handle))

    actual fun createBuffer(size: Long, usage: BufferUsage, label: String?): GpuBuffer = GpuBuffer(
        deviceCreateBuffer(
            handle,
            jsObject {
                this["label"] = label
                this["size"] = size
                this["usage"] = usage.bits
            },
        ),
        size,
    )

    actual fun createTexture(spec: TextureSpec): GpuTexture = GpuTexture(
        deviceCreateTexture(
            handle,
            jsObject {
                this["label"] = spec.label
                this["size"] = jsObject {
                    this["width"] = spec.width
                    this["height"] = spec.height
                }
                this["format"] = spec.format.webName
                this["usage"] = spec.usage.bits
                this["sampleCount"] = spec.sampleCount
            },
        ),
        spec.width,
        spec.height,
        spec.format,
    )

    actual fun createShaderModule(wgsl: String, label: String?): GpuShaderModule = GpuShaderModule(
        deviceCreateShader(
            handle,
            jsObject {
                this["label"] = label
                this["code"] = wgsl
            },
        ),
    )

    actual fun createRenderPipeline(spec: RenderPipelineSpec): GpuRenderPipeline =
        GpuRenderPipeline(deviceCreatePipeline(handle, pipelineDescriptor(spec)))

    actual fun createBindGroup(layout: GpuBindGroupLayout, buffers: List<BufferBinding>, label: String?): GpuBindGroup = GpuBindGroup(
        deviceCreateBindGroup(
            handle,
            jsObject {
                this["label"] = label
                this["layout"] = layout.handle
                this["entries"] = buffers.toJs { binding ->
                    jsObject {
                        this["binding"] = binding.binding
                        this["resource"] = jsObject {
                            this["buffer"] = binding.buffer.handle
                            this["offset"] = binding.offset
                            this["size"] = binding.size
                        }
                    }
                }
            },
        ),
    )

    actual fun createCommandEncoder(): GpuCommandEncoder = GpuCommandEncoder(deviceCreateEncoder(handle))

    actual suspend fun readBuffer(buffer: GpuBuffer, size: Long): ByteArray {
        val bytes = readMapped(buffer.handle, MAP_READ, size.toDouble()).await<Uint8Array>()
        return ByteArray(bytes.length) { bytes[it] }
    }

    actual override fun close() = deviceDestroy(handle)
}

private fun pipelineDescriptor(spec: RenderPipelineSpec): JsAny = jsObject {
    this["label"] = spec.label
    this["layout"] = AUTO_LAYOUT
    this["vertex"] = jsObject {
        this["module"] = spec.shader.handle
        this["entryPoint"] = spec.vertexEntry
        this["buffers"] = spec.buffers.toJs { buffer ->
            jsObject {
                this["arrayStride"] = buffer.stride
                this["stepMode"] = buffer.stepMode.webName
                this["attributes"] = buffer.attributes.toJs { attribute ->
                    jsObject {
                        this["format"] = attribute.format.webName
                        this["offset"] = attribute.offset
                        this["shaderLocation"] = attribute.location
                    }
                }
            }
        }
    }
    this["primitive"] = jsObject {
        this["topology"] = spec.topology.webName
        this["cullMode"] = spec.cullMode.webName
    }
    this["depthStencil"] = spec.depth?.let { depth ->
        jsObject {
            this["format"] = depth.format.webName
            this["depthWriteEnabled"] = depth.write
            this["depthCompare"] = depth.compare.webName
        }
    }
    this["multisample"] = jsObject { this["count"] = spec.sampleCount }
    this["fragment"] = spec.fragmentEntry?.let { entry ->
        jsObject {
            this["module"] = spec.shader.handle
            this["entryPoint"] = entry
            this["targets"] = spec.targets.toJs { target ->
                jsObject {
                    this["format"] = target.format.webName
                    this["blend"] = target.blend?.let { blend ->
                        jsObject {
                            this["color"] = blendComponent(blend.color)
                            this["alpha"] = blendComponent(blend.alpha)
                        }
                    }
                }
            }
        }
    }
}

private fun blendComponent(component: BlendComponent): JsAny = jsObject {
    this["operation"] = component.operation.webName
    this["srcFactor"] = component.srcFactor.webName
    this["dstFactor"] = component.dstFactor.webName
}
