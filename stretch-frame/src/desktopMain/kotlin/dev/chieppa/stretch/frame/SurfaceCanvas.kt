package dev.chieppa.stretch.frame

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import java.awt.Canvas
import java.awt.Component
import java.awt.Graphics
import kotlin.math.roundToInt

/**
 * A heavyweight AWT canvas whose native window a [SurfaceRenderer] draws into. The renderer runs on a [SurfaceLoop]
 * from the moment the native window exists until just before it is destroyed, and input passes through the window
 * to Compose (see [HitTestPassThrough]).
 */
internal class SurfaceCanvas<S>(
    private val renderer: SurfaceRenderer<S>,
    initial: S,
    private val onFailure: (Throwable) -> Unit,
) : Canvas() {
    @Volatile private var latest: S = initial
    private var loop: SurfaceLoop<S>? = null
    private var passThrough: HitTestPassThrough? = null

    init {
        ignoreRepaint = true
        isFocusable = false
    }

    fun publish(state: S) {
        latest = state
        loop?.publish(state)
    }

    override fun addNotify() {
        super.addNotify()
        val handle = surfaceHandleOf(this)
        if (handle == null) {
            onFailure(UnsupportedOperationException("Native surfaces are not supported on ${System.getProperty("os.name")}"))
            return
        }
        passThrough = HitTestPassThrough.install(handle)
        loop = SurfaceLoop(handle, renderer, latest, onFailure).also { it.resize(pixelSize()) }
    }

    override fun removeNotify() {
        loop?.close()
        loop = null
        passThrough?.close()
        passThrough = null
        super.removeNotify()
    }

    @Deprecated("Deprecated in AWT; still the one call every move and resize goes through")
    @Suppress("DEPRECATION")
    override fun reshape(x: Int, y: Int, width: Int, height: Int) {
        super.reshape(x, y, width, height)
        loop?.resize(pixelSize())
    }

    override fun paint(g: Graphics?) = Unit

    override fun update(g: Graphics?) = Unit

    private fun pixelSize(): SurfaceSize {
        val scale = graphicsConfiguration?.defaultTransform?.scaleX ?: 1.0
        return SurfaceSize((width * scale).roundToInt(), (height * scale).roundToInt(), scale.toFloat())
    }
}

/** [component]'s native window as a [SurfaceHandle]; null before it has one or where surfaces are not supported. */
internal fun surfaceHandleOf(component: Component): SurfaceHandle? {
    if (!isWindows) return null
    val hwnd = Native.getComponentID(component).takeIf { it != 0L } ?: return null
    val module = Pointer.nativeValue(Kernel32.INSTANCE.GetModuleHandle(null).pointer)
    return SurfaceHandle.Win32(hwnd, module)
}
