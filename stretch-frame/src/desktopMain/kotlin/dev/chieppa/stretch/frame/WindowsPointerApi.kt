package dev.chieppa.stretch.frame

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.ptr.IntByReference

/** Win32 POINTER_INFO: one pointer's state as Windows reports it. */
@Structure.FieldOrder(
    "pointerType", "pointerId", "frameId", "pointerFlags", "sourceDevice", "hwndTarget",
    "ptPixelLocation", "ptHimetricLocation", "ptPixelLocationRaw", "ptHimetricLocationRaw",
    "dwTime", "historyCount", "inputData", "dwKeyStates", "performanceCount", "buttonChangeType",
)
internal open class NativePointerInfo : Structure() {
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

/** Win32 POINTER_PEN_INFO: a pen pointer's state, with its pressure, buttons and tilt. */
@Structure.FieldOrder("pointerInfo", "penFlags", "penMask", "pressure", "rotation", "tiltX", "tiltY")
internal open class NativePenInfo : Structure() {
    @JvmField var pointerInfo = NativePointerInfo()
    @JvmField var penFlags = 0
    @JvmField var penMask = 0
    @JvmField var pressure = 0
    @JvmField var rotation = 0
    @JvmField var tiltX = 0
    @JvmField var tiltY = 0
}

/** The user32 calls the Windows input code makes ([WindowsPointerInput], [HitTestPassThrough]). */
@Suppress("FunctionName")
internal interface PointerUser32 : Library {
    fun GetPointerType(pointerId: Int, pointerType: IntByReference): Boolean
    fun GetPointerInfo(pointerId: Int, info: NativePointerInfo): Boolean
    fun GetPointerPenInfo(pointerId: Int, info: NativePenInfo): Boolean
    fun GetKeyState(key: Int): Short
    fun SetWindowLongPtrW(hWnd: WinDef.HWND, index: Int, proc: WinUser.WindowProc): Pointer?
    fun SetWindowLongPtrW(hWnd: WinDef.HWND, index: Int, proc: Pointer?): Pointer?
    fun GetWindowLongPtrW(hWnd: WinDef.HWND, index: Int): Pointer?
    fun CallWindowProcW(previous: Pointer?, hWnd: WinDef.HWND, message: Int, wParam: WinDef.WPARAM, lParam: WinDef.LPARAM): WinDef.LRESULT
    fun ScreenToClient(hWnd: WinDef.HWND, point: WinDef.POINT): Boolean
    fun EnumChildWindows(parent: WinDef.HWND, callback: WinUser.WNDENUMPROC, data: Pointer?): Boolean

    companion object {
        val instance: PointerUser32 by lazy { Native.load("user32", PointerUser32::class.java) }

        const val GWLP_WNDPROC = -4
        const val PT_TOUCH = 2
        const val PT_PEN = 3
        const val WM_NCHITTEST = 0x0084
        const val HTTRANSPARENT = -1
        const val WM_POINTERUPDATE = 0x0245
        const val WM_POINTERDOWN = 0x0246
        const val WM_POINTERUP = 0x0247
        const val POINTER_FLAG_INCONTACT = 0x4
        const val PEN_FLAG_BARREL = 0x1
        const val PEN_FLAG_INVERTED = 0x2
        const val PEN_FLAG_ERASER = 0x4
        const val PEN_MASK_PRESSURE = 0x1
        const val MAX_PRESSURE = 1024f
        const val VK_SHIFT = 0x10
        const val VK_CONTROL = 0x11
        const val VK_MENU = 0x12
        const val VK_LWIN = 0x5B
        const val VK_RWIN = 0x5C
    }
}
