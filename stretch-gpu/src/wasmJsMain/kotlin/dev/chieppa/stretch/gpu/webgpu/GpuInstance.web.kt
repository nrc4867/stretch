@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.frame.SurfaceHandle
import kotlinx.coroutines.await
import org.w3c.dom.HTMLCanvasElement
import kotlin.js.Promise

private const val OPAQUE = "opaque"
private const val UNKNOWN_GPU = "Unknown GPU"

/** Why a lost device needs no report: it was released on purpose. */
private const val LOST_ON_PURPOSE = "destroyed"

private fun requestAdapter(options: JsAny): Promise<JsAny?> = js("navigator.gpu ? navigator.gpu.requestAdapter(options) : Promise.resolve(null)")

private fun requestDevice(adapter: JsAny): Promise<JsAny> = js("adapter.requestDevice()")

private fun adapterName(adapter: JsAny): String = js("(adapter.info && (adapter.info.description || adapter.info.architecture || adapter.info.vendor)) || ''")

private fun watchErrors(device: JsAny, report: (String) -> Unit): Unit = js("{ device.onuncapturederror = (event) => report(event.error.message); }")

private fun watchLoss(device: JsAny, report: (String, String) -> Unit): Unit = js("{ device.lost.then((info) => report(info.reason, info.message)); }")

private fun preferredFormat(): String = js("navigator.gpu.getPreferredCanvasFormat()")

private fun canvasContext(canvas: HTMLCanvasElement): JsAny? = js("canvas.getContext('webgpu')")

private fun contextConfigure(context: JsAny, configuration: JsAny): Unit = js("context.configure(configuration)")

private fun contextUnconfigure(context: JsAny): Unit = js("context.unconfigure()")

private fun contextTexture(context: JsAny): JsAny = js("context.getCurrentTexture()")

private fun textureWidth(texture: JsAny): Int = js("texture.width")

private fun textureHeight(texture: JsAny): Int = js("texture.height")

/** A canvas's WebGPU context. The browser presents what was drawn when the animation frame ends. */
internal actual class GpuSurface(private val context: JsAny) : AutoCloseable {
    private var format = TextureFormat.BGRA8Unorm

    actual fun configure(device: GpuDevice, format: TextureFormat, width: Int, height: Int, presentMode: PresentMode) {
        contextConfigure(
            context,
            jsObject {
                this["device"] = device.handle
                this["format"] = format.webName
                this["usage"] = TextureUsage.RenderAttachment.bits
                this["alphaMode"] = OPAQUE
            },
        )
        this.format = format
    }

    actual fun acquire(): GpuTexture? {
        val texture = contextTexture(context)
        return GpuTexture(texture, textureWidth(texture), textureHeight(texture), format, owned = false)
    }

    actual fun present(): Boolean = true

    actual override fun close() = contextUnconfigure(context)
}

/** WebGPU as the browser offers it: the browser picks the API underneath, so a backend choice has no effect. */
internal actual class GpuInstance private constructor(
    actual val surface: GpuSurface?,
    actual val device: GpuDevice,
    actual val description: String,
) : AutoCloseable {
    private var lostReason: String? = null

    actual val lost: String? get() = lostReason

    actual fun surfaceFormats(): List<TextureFormat> {
        if (surface == null) return emptyList()
        val preferred = preferredFormat()
        return TextureFormat.entries.filter { it.webName == preferred }
    }

    actual override fun close() {
        surface?.close()
        device.close()
    }

    actual companion object {
        actual suspend fun create(window: SurfaceHandle?, backend: GpuBackend?, power: PowerPreference, onError: (String) -> Unit): GpuInstance {
            val options = jsObject { this["powerPreference"] = power.webName }
            val adapter = requestAdapter(options).await<JsAny?>() ?: error("No GPU adapter suits WebGPU")
            val deviceHandle = requestDevice(adapter).await<JsAny>()
            watchErrors(deviceHandle, onError)
            val surface = (window as? SurfaceHandle.Canvas)?.let {
                GpuSurface(canvasContext(it.element) ?: error("WebGPU could not make a surface for the canvas"))
            }
            val name = adapterName(adapter).ifBlank { UNKNOWN_GPU }
            val gpu = GpuInstance(surface, GpuDevice(deviceHandle), "$name (WebGPU)")
            watchLoss(deviceHandle) { reason, message ->
                if (reason != LOST_ON_PURPOSE) {
                    val text = "The GPU device was lost: $message"
                    gpu.lostReason = text
                    onError(text)
                }
            }
            return gpu
        }
    }
}
