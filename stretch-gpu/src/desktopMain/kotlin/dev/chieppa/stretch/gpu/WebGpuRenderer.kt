package dev.chieppa.stretch.gpu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.chieppa.stretch.frame.SurfaceHandle
import dev.chieppa.stretch.frame.SurfaceRenderer
import dev.chieppa.stretch.frame.SurfaceSize
import dev.chieppa.stretch.gpu.webgpu.GpuInstance
import dev.chieppa.stretch.gpu.webgpu.TextureFormat

/** The color formats a frame prefers, in order: plain 8-bit, so colors match Compose's. */
private val PREFERRED_FORMATS = listOf(TextureFormat.BGRA8Unorm, TextureFormat.RGBA8Unorm)

/**
 * Draws a [GpuScene] into a [dev.chieppa.stretch.frame.NativeSurface] with WebGPU. On attach it sets up a device for
 * the surface and makes the scene with [scene]; each frame it keeps the surface and the frame's targets at the
 * surface's size, lets the scene record its commands, then submits and presents them. A surface with no texture for
 * now is configured again and drawn on the next frame; a lost device or a WebGPU error ends the surface.
 */
class WebGpuRenderer<S>(
    private val options: GpuOptions = GpuOptions(),
    private val scene: (GpuContext) -> GpuScene<S>,
) : SurfaceRenderer<S> {
    private var gpu: GpuInstance? = null
    private var context: GpuContext? = null
    private var targets: RenderTargets? = null
    private var current: GpuScene<S>? = null
    private var configured = SurfaceSize.Zero

    /** The GPU and API in use once attached. Compose state, so a label showing it updates. */
    var description: String? by mutableStateOf(null)
        private set

    override fun attach(handle: SurfaceHandle) {
        val gpu = GpuInstance.create(handle, options.backend, options.powerPreference, options.onError).also { gpu = it }
        val formats = gpu.surfaceFormats()
        val format = PREFERRED_FORMATS.firstOrNull { it in formats } ?: formats.firstOrNull()
            ?: error("The surface offers no format Stretch can draw")
        val context = GpuContext(gpu.device, format, options.sampleCount, gpu.description).also { context = it }
        targets = RenderTargets(context)
        current = scene(context)
        description = gpu.description
    }

    override fun render(state: S, size: SurfaceSize): Boolean {
        val gpu = checkNotNull(gpu) { "Not attached" }
        val context = checkNotNull(context)
        gpu.lost?.let { error(it) }
        val surface = checkNotNull(gpu.surface)
        if (size.width != configured.width || size.height != configured.height) configure(size)
        val texture = surface.acquire()
        if (texture == null) {
            configure(size)
            return true
        }
        // The surface's texture is released only after it is presented: wgpu drops a frame whose texture is gone.
        texture.use {
            val animating = texture.createView().use { output ->
                val targets = checkNotNull(targets).also { it.ensure(size.width, size.height) }
                context.device.createCommandEncoder().use { encoder ->
                    val animating = checkNotNull(current).render(GpuFrame(context, encoder, size, targets, output), state)
                    context.device.queue.submit(encoder.finish())
                    animating
                }
            }
            if (!surface.present()) configured = SurfaceSize.Zero
            return animating
        }
    }

    private fun configure(size: SurfaceSize) {
        val gpu = checkNotNull(gpu)
        checkNotNull(gpu.surface).configure(gpu.device, checkNotNull(context).format, size.width, size.height, options.presentMode)
        configured = size
    }

    override fun detach() {
        current?.close()
        targets?.close()
        context?.close()
        gpu?.close()
        current = null
        targets = null
        context = null
        gpu = null
        configured = SurfaceSize.Zero
    }
}
