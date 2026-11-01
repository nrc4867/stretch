package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeScenePointer
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.ptr.IntByReference
import java.awt.Component
import java.awt.Window
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * Touch on Windows, delivered to Compose as touch. Left alone, the JetBrains Runtime turns a finger drag into
 * scrolling and Windows turns a two-finger tap into a right click, so Compose never sees fingers. This reads the
 * window's touch pointers itself, keeps Windows from turning them into mouse input, and hands each finger to the
 * window's Compose scene as its own touch pointer: taps click, a finger drags handles, two fingers pan and pinch.
 * Pens and mice are untouched. Where the window's scene cannot be reached, nothing changes.
 */
@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
internal class WindowsTouch private constructor(
    private val window: Window,
    private val scene: ComposeScene,
    private val mediator: Any,
    private val container: JComponent,
    private val canvas: Component,
) : AutoCloseable {
    private val api = TouchUser32.instance
    private val originals = HashMap<Pointer, Pointer>()
    private val procedures = HashMap<Pointer, WinUser.WindowProc>()

    /** Fingers on the glass, by Windows pointer id, in scene pixels. Only touched on the event thread. */
    private val fingers = LinkedHashMap<Int, Offset>()

    private fun attach() {
        val root = WinDef.HWND(Native.getWindowPointer(window) ?: return)
        subclass(root)
        api.EnumChildWindows(root, { child, _ -> subclass(child); true }, null)
    }

    private fun subclass(hwnd: WinDef.HWND) {
        val key = hwnd.pointer
        if (originals.containsKey(key)) return
        val procedure = WinUser.WindowProc { target, message, wParam, lParam ->
            if ((message == WM_POINTERDOWN || message == WM_POINTERUPDATE || message == WM_POINTERUP) && isTouch(wParam)) {
                runCatching { read(message, wParam) }
                // Handled: Windows makes no mouse clicks, scrolling or right clicks out of it.
                WinDef.LRESULT(0)
            } else {
                api.CallWindowProcW(originals[key], target, message, wParam, lParam)
            }
        }
        procedures[key] = procedure
        val original = api.SetWindowLongPtrW(hwnd, GWLP_WNDPROC, procedure) ?: return
        originals[key] = original
    }

    private fun isTouch(wParam: WinDef.WPARAM): Boolean {
        val type = IntByReference()
        return api.GetPointerType((wParam.toLong() and 0xFFFF).toInt(), type) && type.value == PT_TOUCH
    }

    private fun read(message: Int, wParam: WinDef.WPARAM) {
        val id = (wParam.toLong() and 0xFFFF).toInt()
        val info = TouchPointerInfo()
        if (!api.GetPointerInfo(id, info)) return
        val screen = WinDef.POINT(info.ptPixelLocation.x, info.ptPixelLocation.y)
        val time = System.currentTimeMillis()
        SwingUtilities.invokeLater { deliver(message, id, screen, time) }
    }

    /** Sends one finger's change to the scene, with every other finger as it is, as Compose expects. */
    private fun deliver(message: Int, id: Int, screen: WinDef.POINT, time: Long) {
        if (!canvas.isShowing) return
        val at = toScene(screen) ?: return
        val type = when (message) {
            WM_POINTERDOWN -> PointerEventType.Press
            WM_POINTERUP -> PointerEventType.Release
            else -> if (id in fingers) PointerEventType.Move else return
        }
        fingers[id] = at
        val pointers = fingers.map { (finger, position) ->
            ComposeScenePointer(PointerId(TOUCH_ID_BASE + finger), position, pressed = !(finger == id && type == PointerEventType.Release), type = PointerType.Touch)
        }
        if (type == PointerEventType.Release) fingers.remove(id)
        scene.sendPointerEvent(type, pointers, timeMillis = time)
    }

    /**
     * A physical screen point in the scene's pixels, as the scene's own mouse handling measures them. The scene's
     * canvas may be lightweight, with no native window of its own, so the point is measured against the nearest
     * heavyweight ancestor (the canvas itself, or at the latest the window) and carried over to the canvas.
     */
    private fun toScene(screen: WinDef.POINT): Offset? {
        val native = generateSequence(canvas) { it.parent }.firstOrNull { !it.isLightweight } ?: return null
        val hwnd = WinDef.HWND(Native.getComponentPointer(native) ?: return null)
        if (!api.ScreenToClient(hwnd, screen)) return null
        val density = window.graphicsConfiguration.defaultTransform.scaleX.toFloat()
        // A window's client area starts inside its insets; any other heavyweight's starts at its own origin.
        val insets = (native as? Window)?.insets
        val inNative = SwingUtilities.convertPoint(canvas, 0, 0, native)
        val inContainer = SwingUtilities.convertPoint(canvas, 0, 0, container)
        val shiftX = (insets?.left ?: 0) - inNative.x + inContainer.x
        val shiftY = (insets?.top ?: 0) - inNative.y + inContainer.y
        val bounds = runCatching { mediator.javaClass.getMethod("getSceneBoundsInPx").invoke(mediator) as Rect? }.getOrNull()
        return Offset(screen.x + shiftX * density, screen.y + shiftY * density) - (bounds?.topLeft ?: Offset.Zero)
    }

    override fun close() {
        originals.forEach { (hwnd, original) -> api.SetWindowLongPtrW(WinDef.HWND(hwnd), GWLP_WNDPROC, original) }
        originals.clear()
        procedures.clear()
    }

    companion object {
        private val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows")

        /** Ids for touch pointers, clear of the ids Compose gives the mouse. */
        private const val TOUCH_ID_BASE = 1L shl 32

        /**
         * Starts delivering touch to [window], a Compose window or dialog that is showing. Null on other platforms,
         * or when this version of Compose keeps its scene somewhere else.
         */
        fun install(window: Window): WindowsTouch? {
            if (!isWindows || !window.isShowing) return null
            return runCatching {
                val panel = field(window, "composePanel")
                val container = field(panel, "_composeContainer")
                val mediator = field(container, "mediator")
                val scene = (field(mediator, "scene\$delegate") as Lazy<*>).value as ComposeScene
                val composeContainer = field(mediator, "container") as JComponent
                val canvas = mediator.javaClass.getMethod("getContentComponent").invoke(mediator) as Component
                WindowsTouch(window, scene, mediator, composeContainer, canvas).also { it.attach() }
            }.getOrNull()
        }

        private fun field(owner: Any, name: String): Any {
            var type: Class<*>? = owner.javaClass
            while (type != null) {
                runCatching { return type.getDeclaredField(name).apply { isAccessible = true }.get(owner) }
                type = type.superclass
            }
            error("No field $name on ${owner.javaClass.name}")
        }

        private const val GWLP_WNDPROC = -4
        private const val PT_TOUCH = 2
        private const val WM_POINTERUPDATE = 0x0245
        private const val WM_POINTERDOWN = 0x0246
        private const val WM_POINTERUP = 0x0247
    }
}

@Structure.FieldOrder(
    "pointerType", "pointerId", "frameId", "pointerFlags", "sourceDevice", "hwndTarget",
    "ptPixelLocation", "ptHimetricLocation", "ptPixelLocationRaw", "ptHimetricLocationRaw",
    "dwTime", "historyCount", "inputData", "dwKeyStates", "performanceCount", "buttonChangeType",
)
internal open class TouchPointerInfo : Structure() {
    @JvmField var pointerType = 0
    @JvmField var pointerId = 0
    @JvmField var frameId = 0
    @JvmField var pointerFlags = 0
    @JvmField var sourceDevice: Pointer? = null
    @JvmField var hwndTarget: Pointer? = null
    @JvmField var ptPixelLocation = WinDef.POINT()
    @JvmField var ptHimetricLocation = WinDef.POINT()
    @JvmField var ptPixelLocationRaw = WinDef.POINT()
    @JvmField var ptHimetricLocationRaw = WinDef.POINT()
    @JvmField var dwTime = 0
    @JvmField var historyCount = 0
    @JvmField var inputData = 0
    @JvmField var dwKeyStates = 0
    @JvmField var performanceCount = 0L
    @JvmField var buttonChangeType = 0
}

@Suppress("FunctionName")
internal interface TouchUser32 : Library {
    fun GetPointerType(pointerId: Int, pointerType: IntByReference): Boolean
    fun GetPointerInfo(pointerId: Int, info: TouchPointerInfo): Boolean
    fun SetWindowLongPtrW(hWnd: WinDef.HWND, index: Int, proc: WinUser.WindowProc): Pointer?
    fun SetWindowLongPtrW(hWnd: WinDef.HWND, index: Int, proc: Pointer?): Pointer?
    fun CallWindowProcW(previous: Pointer?, hWnd: WinDef.HWND, message: Int, wParam: WinDef.WPARAM, lParam: WinDef.LPARAM): WinDef.LRESULT
    fun ScreenToClient(hWnd: WinDef.HWND, point: WinDef.POINT): Boolean
    fun EnumChildWindows(parent: WinDef.HWND, callback: WinUser.WNDENUMPROC, data: Pointer?): Boolean

    companion object {
        val instance: TouchUser32 by lazy { Native.load("user32", TouchUser32::class.java) }
    }
}

/** Delivers [window]'s touch to Compose as touch while this is composed (see [WindowsTouch]). */
@Composable
internal fun TouchInput(window: Window) {
    DisposableEffect(window) {
        var touch: WindowsTouch? = null
        var tries = 0
        // The window's native handles exist once it is showing.
        val timer = Timer(16, null)
        timer.addActionListener {
            touch = WindowsTouch.install(window)
            if (touch != null || ++tries > 300) timer.stop()
        }
        timer.start()
        onDispose {
            timer.stop()
            touch?.close()
        }
    }
}
