package dev.chieppa.stretch.gpu

import dev.chieppa.stretch.frame.SurfaceHandle
import dev.chieppa.stretch.frame.SurfaceRenderer
import dev.chieppa.stretch.frame.SurfaceSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Draws a [GpuScene] into a [dev.chieppa.stretch.frame.NativeSurface] with WebGPU. On attach it sets up a device for
 * the surface and makes the scene with [scene]; each frame it keeps the surface and the frame's targets at the
 * surface's size, lets the scene record its commands, then submits and presents them. A surface that gives no texture
 * is reconfigured and drawn again on the next frame; anything WebGPU throws ends the surface with an error.
 */
class WebGpuRenderer<S>(
    private val options: GpuOptions = GpuOptions(),
    private val scene: (GpuContext) -> GpuScene<S>,
) : SurfaceRenderer<S> {
    private var session: GpuSession? = null
    private var context: GpuContext? = null
    private var targets: RenderTargets? = null
    private var current: GpuScene<S>? = null

    /** The GPU in use once attached, and the backend asked for. Compose state, so a label showing it updates. */
    var description: String? by mutableStateOf(null)
        private set

    override fun attach(handle: SurfaceHandle) {
        val session = GpuSession.forSurface(handle, options).also { session = it }
        val context = GpuContext(session.device, session.format, options.sampleCount, session.description).also { context = it }
        targets = RenderTargets(context)
        current = scene(context)
        description = session.description
    }

    override fun render(state: S, size: SurfaceSize): Boolean {
        val session = checkNotNull(session) { "Not attached" }
        val surface = checkNotNull(session.surface)
        session.configure(size)
        val texture = surface.acquireTexture()
        if (texture == null) {
            session.configure(size, force = true)
            return true
        }
        val output = texture.createView()
        val targets = checkNotNull(targets).also { it.ensure(size.width, size.height) }
        val encoder = session.device.createCommandEncoder()
        val animating = checkNotNull(current).render(GpuFrame(checkNotNull(context), encoder, size, targets, output), state)
        val commands = encoder.finish()
        session.device.queue.submit(listOf(commands))
        surface.present()
        commands.close()
        encoder.close()
        output.close()
        texture.close()
        return animating
    }

    override fun detach() {
        current?.close()
        targets?.close()
        context?.close()
        session?.close()
        current = null
        targets = null
        context = null
        session = null
    }
}
