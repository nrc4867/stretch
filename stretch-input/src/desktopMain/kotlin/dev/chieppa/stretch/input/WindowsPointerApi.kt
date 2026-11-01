package dev.chieppa.stretch.input

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference

@Structure.FieldOrder(
    "pointerType", "pointerId", "frameId", "pointerFlags", "sourceDevice", "hwndTarget",
    "ptPixelLocation", "ptHimetricLocation", "ptPixelLocationRaw", "ptHimetricLocationRaw",
    "dwTime", "historyCount", "inputData", "dwKeyStates", "performanceCount", "buttonChangeType",
)
internal open class PointerInfo : Structure() {
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

@Structure.FieldOrder("pointerInfo", "penFlags", "penMask", "pressure", "rotation", "tiltX", "tiltY")
internal open class PenInfo : Structure() {
    @JvmField var pointerInfo = PointerInfo()
    @JvmField var penFlags = 0
    @JvmField var penMask = 0
    @JvmField var pressure = 0
    @JvmField var rotation = 0
    @JvmField var tiltX = 0
    @JvmField var tiltY = 0
}

@Suppress("FunctionName")
internal interface User32Pointer : Library {
    fun GetPointerType(pointerId: Int, pointerType: IntByReference): Boolean
    fun GetPointerPenInfo(pointerId: Int, penInfo: PenInfo): Boolean
    fun GetPointerPenInfoHistory(pointerId: Int, entriesCount: IntByReference, penInfo: PenInfo?): Boolean
    fun GetPointerDeviceRects(device: Pointer?, pointerDeviceRect: WinDef.RECT, displayRect: WinDef.RECT): Boolean
    fun SetWindowLongPtrW(hWnd: WinDef.HWND, index: Int, proc: WinUser.WindowProc): Pointer?
    fun SetWindowLongPtrW(hWnd: WinDef.HWND, index: Int, proc: Pointer?): Pointer?
    fun CallWindowProcW(previous: Pointer?, hWnd: WinDef.HWND, message: Int, wParam: WinDef.WPARAM, lParam: WinDef.LPARAM): WinDef.LRESULT
    fun ClientToScreen(hWnd: WinDef.HWND, point: WinDef.POINT): Boolean
    fun EnumChildWindows(parent: WinDef.HWND, callback: WinUser.WNDENUMPROC, data: Pointer?): Boolean

    companion object {
        val instance: User32Pointer by lazy { Native.load("user32", User32Pointer::class.java) }

        const val GWLP_WNDPROC = -4
        const val PT_PEN = 3
        const val WM_POINTERUPDATE = 0x0245
        const val WM_POINTERDOWN = 0x0246
        const val WM_POINTERUP = 0x0247
        const val WM_POINTERLEAVE = 0x024A
        const val POINTER_FLAG_INCONTACT = 0x4
        const val PEN_FLAG_BARREL = 0x1
        const val PEN_FLAG_INVERTED = 0x2
        const val PEN_FLAG_ERASER = 0x4
        const val PEN_MASK_PRESSURE = 0x1
        const val PEN_MASK_TILT_X = 0x4
        const val PEN_MASK_TILT_Y = 0x8
        const val MAX_PRESSURE = 1024f
    }
}

@Suppress("FunctionName")
internal interface Kernel32Counter : Library {
    fun QueryPerformanceCounter(counter: LongByReference): Boolean
    fun QueryPerformanceFrequency(frequency: LongByReference): Boolean

    companion object {
        val instance: Kernel32Counter by lazy { Native.load("kernel32", Kernel32Counter::class.java) }
    }
}
