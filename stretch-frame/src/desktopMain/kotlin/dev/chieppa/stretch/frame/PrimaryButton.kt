package dev.chieppa.stretch.frame

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent

/** How long a window has to stay still before its drag counts as over, where the button cannot be asked. */
private const val SETTLE_NANOS = 350_000_000L

/**
 * Whether a window drag that last moved the window at [movedAt] (from [System.nanoTime]) is still going on: whether
 * the primary button is held, where that can be told, or else whether the window moved a moment ago.
 */
internal fun windowDragGoingOn(movedAt: Long): Boolean =
    PrimaryButton.held() ?: (System.nanoTime() - movedAt < SETTLE_NANOS)

/**
 * The primary mouse button's state from the OS, which still knows it while the OS itself drags a window and the app
 * gets no mouse events: Windows through User32, macOS through CoreGraphics. Elsewhere it comes from AWT's own mouse
 * events, which can tell only that a press inside the app is still held.
 */
internal object PrimaryButton {
    /** Whether the primary button is held, or null where that cannot be told. */
    fun held(): Boolean? = when {
        isWindows -> windowsHeld()
        isMac -> macHeld() ?: AwtButton.held()
        else -> AwtButton.held()
    }

    private fun windowsHeld(): Boolean? = runCatching {
        val button = if (User32.INSTANCE.GetSystemMetrics(SM_SWAPBUTTON) != 0) VK_RBUTTON else VK_LBUTTON
        User32.INSTANCE.GetAsyncKeyState(button).toInt() and 0x8000 != 0
    }.getOrNull()

    private fun macHeld(): Boolean? = CoreGraphics.library?.let { cg ->
        runCatching { cg.CGEventSourceButtonState(CG_COMBINED_SESSION_STATE, CG_LEFT_BUTTON).toInt() != 0 }.getOrNull()
    }

    private const val SM_SWAPBUTTON = 23
    private const val VK_LBUTTON = 1
    private const val VK_RBUTTON = 2
    private const val CG_COMBINED_SESSION_STATE = 0
    private const val CG_LEFT_BUTTON = 0
}

/** The CoreGraphics call that reports a mouse button's state on macOS. */
@Suppress("FunctionName")
private interface CoreGraphics : Library {
    /** `bool CGEventSourceButtonState(CGEventSourceStateID, CGMouseButton)`; a C bool is one byte. */
    fun CGEventSourceButtonState(state: Int, button: Int): Byte

    companion object {
        val library: CoreGraphics? by lazy { runCatching { Native.load("CoreGraphics", CoreGraphics::class.java) }.getOrNull() }
    }
}

/**
 * The primary button as AWT's mouse events in this app show it. A press seen and not yet released means held; with
 * no such press it cannot tell, since the OS may be dragging a window and keeping the events to itself.
 */
private object AwtButton {
    @Volatile
    private var pressed = false

    private val listener = AWTEventListener { event ->
        if (event is MouseEvent && event.button == MouseEvent.BUTTON1) {
            when (event.id) {
                MouseEvent.MOUSE_PRESSED -> pressed = true
                MouseEvent.MOUSE_RELEASED -> pressed = false
            }
        }
    }

    private val installed: Boolean by lazy {
        runCatching { Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.MOUSE_EVENT_MASK) }.isSuccess
    }

    fun held(): Boolean? = if (installed && pressed) true else null

    /** Starts listening, so a press before the first question is seen. */
    fun install() {
        installed
    }
}

/** Starts following AWT's mouse events where the OS cannot be asked for the button, ahead of any window drag. */
internal fun watchPrimaryButton() {
    if (!isWindows) AwtButton.install()
}
