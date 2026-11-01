package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeScenePointer
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.ptr.IntByReference
import dev.chieppa.stretch.frame.PointerUser32.Companion.GWLP_WNDPROC
import dev.chieppa.stretch.frame.PointerUser32.Companion.MAX_PRESSURE
import dev.chieppa.stretch.frame.PointerUser32.Companion.PEN_FLAG_BARREL
import dev.chieppa.stretch.frame.PointerUser32.Companion.PEN_FLAG_ERASER
import dev.chieppa.stretch.frame.PointerUser32.Companion.PEN_FLAG_INVERTED
import dev.chieppa.stretch.frame.PointerUser32.Companion.PEN_MASK_PRESSURE
import dev.chieppa.stretch.frame.PointerUser32.Companion.POINTER_FLAG_INCONTACT
import dev.chieppa.stretch.frame.PointerUser32.Companion.PT_PEN
import dev.chieppa.stretch.frame.PointerUser32.Companion.PT_TOUCH
import dev.chieppa.stretch.frame.PointerUser32.Companion.VK_CONTROL
import dev.chieppa.stretch.frame.PointerUser32.Companion.VK_LWIN
import dev.chieppa.stretch.frame.PointerUser32.Companion.VK_MENU
import dev.chieppa.stretch.frame.PointerUser32.Companion.VK_RWIN
import dev.chieppa.stretch.frame.PointerUser32.Companion.VK_SHIFT
import dev.chieppa.stretch.frame.PointerUser32.Companion.WM_POINTERDOWN
import dev.chieppa.stretch.frame.PointerUser32.Companion.WM_POINTERUP
import dev.chieppa.stretch.frame.PointerUser32.Companion.WM_POINTERUPDATE
import java.awt.Component
import java.awt.Window
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * Touch and pens on Windows, delivered to Compose as touch and stylus. Left alone, the JetBrains Runtime turns a
 * finger drag into scrolling, Windows turns a two-finger tap into a right click, and Windows pans a desktop app under
 * a pen dragged across it, so Compose never sees fingers and a pen stroke scrolls the view. This reads the window's
 * fingers and pens in contact itself, keeps Windows from turning them into mouse or scroll input, and hands each to
 * the window's Compose scene as its own pointer: taps click, a finger or pen drags handles, two fingers pan and
 * pinch, a pen's pressure comes through as the pointer's pressure, its eraser end as [PointerType.Eraser] and its
 * barrel button as the secondary button. A hovering pen is left to Windows, which moves the mouse pointer with it,
 * so hover and pointer shapes still work. Mice are untouched. Where the window's scene cannot be reached, nothing
 * changes.
 *
 * Something else reading the same window's pen messages natively, such as a stretch-input pen source, sees pens in
 * contact only if it attaches after this does, so that its window procedure runs first.
 */
@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
internal class WindowsPointerInput private constructor(
    private val window: Window,
    private val scene: ComposeScene,
    private val mediator: Any,
    private val container: JComponent,
    private val canvas: Component,
) : AutoCloseable {
    private val api = PointerUser32.instance
    private val originals = HashMap<Pointer, Pointer>()
    private val procedures = HashMap<Pointer, WinUser.WindowProc>()

    /** Pens in contact that this handles, by Windows pointer id. Only touched on the window's own thread. */
    private val pensDown = HashSet<Int>()

    /** Fingers and pens down, by Windows pointer id, as last sent to the scene. Only touched on the event thread. */
    private val contacts = LinkedHashMap<Int, Contact>()

    private data class Contact(val position: Offset, val type: PointerType, val pressure: Float, val barrel: Boolean)

    /** One pointer's change, read on the window's thread and delivered on the event thread. */
    private class Reading(
        val message: Int,
        val id: Int,
        val screen: WinDef.POINT,
        val type: PointerType,
        val pressure: Float,
        val barrel: Boolean,
        val modifiers: PointerKeyboardModifiers,
        val time: Long,
    )

    private fun attach() {
        val root = WinDef.HWND(Native.getWindowPointer(window) ?: return)
        subclass(root)
        api.EnumChildWindows(root, { child, _ -> subclass(child); true }, null)
    }

    private fun subclass(hwnd: WinDef.HWND) {
        val key = hwnd.pointer
        if (originals.containsKey(key)) return
        val procedure = WinUser.WindowProc { target, message, wParam, lParam ->
            val reading = if (message == WM_POINTERDOWN || message == WM_POINTERUPDATE || message == WM_POINTERUP) {
                runCatching { read(message, wParam) }.getOrNull()
            } else {
                null
            }
            if (reading != null) {
                SwingUtilities.invokeLater { deliver(reading) }
                // Handled: Windows makes no mouse clicks, scrolling, panning or right clicks out of it.
                WinDef.LRESULT(0)
            } else {
                api.CallWindowProcW(originals[key], target, message, wParam, lParam)
            }
        }
        procedures[key] = procedure
        val original = api.SetWindowLongPtrW(hwnd, GWLP_WNDPROC, procedure) ?: return
        originals[key] = original
    }

    /** The change [message] brings for the pointer in [wParam], or null to leave it to Windows. */
    private fun read(message: Int, wParam: WinDef.WPARAM): Reading? {
        val id = (wParam.toLong() and 0xFFFF).toInt()
        val kind = IntByReference()
        if (!api.GetPointerType(id, kind)) return null
        val time = System.currentTimeMillis()
        return when (kind.value) {
            PT_TOUCH -> {
                val info = NativePointerInfo()
                if (!api.GetPointerInfo(id, info)) return null
                Reading(message, id, location(info), PointerType.Touch, 1f, false, modifiers(), time)
            }
            PT_PEN -> {
                val info = NativePenInfo()
                if (!api.GetPointerPenInfo(id, info)) return null
                val contact = info.pointerInfo.pointerFlags and POINTER_FLAG_INCONTACT != 0
                if (!isPenContact(message, contact, id in pensDown)) return null
                if (message == WM_POINTERDOWN) pensDown += id
                if (message == WM_POINTERUP) pensDown -= id
                val eraser = info.penFlags and (PEN_FLAG_ERASER or PEN_FLAG_INVERTED) != 0
                val pressure = if (info.penMask and PEN_MASK_PRESSURE != 0) info.pressure / MAX_PRESSURE else 1f
                Reading(
                    message, id, location(info.pointerInfo), if (eraser) PointerType.Eraser else PointerType.Stylus,
                    pressure, info.penFlags and PEN_FLAG_BARREL != 0, modifiers(), time,
                )
            }
            else -> null
        }
    }

    private fun location(info: NativePointerInfo) = WinDef.POINT(info.ptPixelLocation.x, info.ptPixelLocation.y)

    /** The keyboard modifiers held now, as this window's thread sees them. */
    private fun modifiers(): PointerKeyboardModifiers {
        fun held(key: Int) = api.GetKeyState(key).toInt() and 0x8000 != 0
        return PointerKeyboardModifiers(
            isCtrlPressed = held(VK_CONTROL),
            isMetaPressed = held(VK_LWIN) || held(VK_RWIN),
            isAltPressed = held(VK_MENU),
            isShiftPressed = held(VK_SHIFT),
        )
    }

    /** Sends one pointer's change to the scene, with every other finger and pen as it is, as Compose expects. */
    private fun deliver(reading: Reading) {
        if (!canvas.isShowing) return
        val at = toScene(reading.screen) ?: return
        val type = when (reading.message) {
            WM_POINTERDOWN -> PointerEventType.Press
            WM_POINTERUP -> PointerEventType.Release
            else -> if (reading.id in contacts) PointerEventType.Move else return
        }
        val releasing = type == PointerEventType.Release
        contacts[reading.id] = Contact(at, reading.type, reading.pressure, reading.barrel)
        val pointers = contacts.map { (id, c) ->
            ComposeScenePointer(PointerId(NATIVE_ID_BASE + id), c.position, pressed = !(id == reading.id && releasing), type = c.type, pressure = c.pressure)
        }
        if (releasing) contacts.remove(reading.id)
        val pens = contacts.values.filter { it.type != PointerType.Touch }
        scene.sendPointerEvent(
            type,
            pointers,
            buttons = PointerButtons(isPrimaryPressed = pens.any { !it.barrel }, isSecondaryPressed = pens.any { it.barrel }),
            keyboardModifiers = reading.modifiers,
            timeMillis = reading.time,
            button = if (reading.type == PointerType.Touch || type == PointerEventType.Move) null else penButton(reading.barrel),
        )
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

        /** Ids for fingers and pens, clear of the ids Compose gives the mouse. */
        private const val NATIVE_ID_BASE = 1L shl 32

        /**
         * Starts delivering touch and pens to [window], a Compose window or dialog that is showing. Null on other
         * platforms, or when this version of Compose keeps its scene somewhere else.
         */
        fun install(window: Window): WindowsPointerInput? {
            if (!isWindows || !window.isShowing) return null
            return runCatching {
                val panel = field(window, "composePanel")
                val container = field(panel, "_composeContainer")
                val mediator = field(container, "mediator")
                val scene = (field(mediator, "scene\$delegate") as Lazy<*>).value as ComposeScene
                val composeContainer = field(mediator, "container") as JComponent
                val canvas = mediator.javaClass.getMethod("getContentComponent").invoke(mediator) as Component
                WindowsPointerInput(window, scene, mediator, composeContainer, canvas).also { it.attach() }
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
    }
}

/**
 * Whether a pen's [message] is one of its strokes, for [WindowsPointerInput] to handle rather than Windows: the
 * press, every move while that pen is [down] and in [contact], and the lift. Hovering is left to Windows.
 */
internal fun isPenContact(message: Int, contact: Boolean, down: Boolean): Boolean = when (message) {
    WM_POINTERDOWN -> true
    WM_POINTERUP -> down
    WM_POINTERUPDATE -> down && contact
    else -> false
}

/** The button a pen presses or lifts with: the secondary one with its barrel button held, otherwise the primary. */
internal fun penButton(barrel: Boolean): PointerButton = if (barrel) PointerButton.Secondary else PointerButton.Primary

/** Delivers [window]'s touch and pens to Compose as touch and stylus while this is composed (see [WindowsPointerInput]). */
@Composable
internal fun NativePointerInput(window: Window) {
    DisposableEffect(window) {
        var input: WindowsPointerInput? = null
        var tries = 0
        // The window's native handles exist once it is showing.
        val timer = Timer(16, null)
        timer.addActionListener {
            input = WindowsPointerInput.install(window)
            if (input != null || ++tries > 300) timer.stop()
        }
        timer.start()
        onDispose {
            timer.stop()
            input?.close()
        }
    }
}
