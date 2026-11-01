package dev.chieppa.stretch.input

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference
import dev.chieppa.stretch.input.User32Pointer.Companion.GWLP_WNDPROC
import dev.chieppa.stretch.input.User32Pointer.Companion.MAX_PRESSURE
import dev.chieppa.stretch.input.User32Pointer.Companion.PEN_FLAG_BARREL
import dev.chieppa.stretch.input.User32Pointer.Companion.PEN_FLAG_ERASER
import dev.chieppa.stretch.input.User32Pointer.Companion.PEN_FLAG_INVERTED
import dev.chieppa.stretch.input.User32Pointer.Companion.PEN_MASK_PRESSURE
import dev.chieppa.stretch.input.User32Pointer.Companion.PEN_MASK_TILT_X
import dev.chieppa.stretch.input.User32Pointer.Companion.PEN_MASK_TILT_Y
import dev.chieppa.stretch.input.User32Pointer.Companion.POINTER_FLAG_INCONTACT
import dev.chieppa.stretch.input.User32Pointer.Companion.PT_PEN
import dev.chieppa.stretch.input.User32Pointer.Companion.WM_POINTERDOWN
import dev.chieppa.stretch.input.User32Pointer.Companion.WM_POINTERLEAVE
import dev.chieppa.stretch.input.User32Pointer.Companion.WM_POINTERUP
import dev.chieppa.stretch.input.User32Pointer.Companion.WM_POINTERUPDATE
import java.awt.Window
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/** The pen source for [window] on this platform, or null where pens are not read natively yet. */
fun penSourceFor(window: Window): PenSource? =
    if (System.getProperty("os.name").startsWith("Windows")) WindowsPenSource(window) else null

/**
 * Windows Ink pen input, read by subclassing the window procedure of an AWT window and its child windows. Messages
 * still reach the original procedure, so the UI keeps getting the mouse events Windows derives from the pen.
 */
internal class WindowsPenSource(private val window: Window) : PenSource {
    private val api = User32Pointer.instance
    private val queue = ConcurrentLinkedQueue<PointerSample>()
    private val originals = ConcurrentHashMap<Pointer, Pointer>()
    private val procedures = ConcurrentHashMap<Pointer, WinUser.WindowProc>()
    private val counterFrequency = LongByReference().also { Kernel32Counter.instance.QueryPerformanceFrequency(it) }.value

    @Volatile private var inContact = false

    override fun attach() {
        val root = WinDef.HWND(Native.getWindowPointer(window))
        subclass(root)
        api.EnumChildWindows(root, { child, _ -> subclass(child); true }, null)
    }

    override fun poll(): PointerSample? = queue.poll()

    override fun close() {
        originals.forEach { (hwnd, original) -> api.SetWindowLongPtrW(WinDef.HWND(hwnd), GWLP_WNDPROC, original) }
        originals.clear()
        procedures.clear()
    }

    private fun subclass(hwnd: WinDef.HWND) {
        val key = hwnd.pointer
        if (originals.containsKey(key)) return
        val procedure = WinUser.WindowProc { target, message, wParam, lParam ->
            if (message in WM_POINTERUPDATE..WM_POINTERLEAVE) runCatching { handle(target, message, wParam) }
            api.CallWindowProcW(originals[key], target, message, wParam, lParam)
        }
        procedures[key] = procedure
        val original = api.SetWindowLongPtrW(hwnd, GWLP_WNDPROC, procedure) ?: return
        originals[key] = original
    }

    private fun handle(hwnd: WinDef.HWND, message: Int, wParam: WinDef.WPARAM) {
        val pointerId = (wParam.toLong() and 0xFFFF).toInt()
        val type = IntByReference()
        if (!api.GetPointerType(pointerId, type) || type.value != PT_PEN) return
        if (message != WM_POINTERUPDATE && message != WM_POINTERDOWN && message != WM_POINTERUP && message != WM_POINTERLEAVE) return

        val latest = PenInfo()
        if (!api.GetPointerPenInfo(pointerId, latest)) return
        val history = readHistory(pointerId, latest.pointerInfo.historyCount) ?: arrayOf(latest)
        val origin = WinDef.POINT(0, 0).also { api.ClientToScreen(hwnd, it) }
        val mapping = deviceMapping(latest.pointerInfo.sourceDevice)
        for (index in history.indices.reversed()) {
            val left = message == WM_POINTERLEAVE && index == 0
            queue.add(sample(history[index], origin, mapping, coalesced = index > 0, left = left))
        }
    }

    private fun readHistory(pointerId: Int, count: Int): Array<PenInfo>? {
        if (count <= 1) return null
        @Suppress("UNCHECKED_CAST")
        val entries = PenInfo().toArray(count) as Array<PenInfo>
        val read = IntByReference(count)
        if (!api.GetPointerPenInfoHistory(pointerId, read, entries[0])) return null
        val filled = read.value.coerceAtMost(count)
        for (i in 0 until filled) entries[i].read()
        return entries.copyOf(filled).requireNoNulls()
    }

    private fun sample(
        info: PenInfo,
        origin: WinDef.POINT,
        mapping: Pair<AxisMapping, AxisMapping>?,
        coalesced: Boolean,
        left: Boolean,
    ): PointerSample {
        val pointer = info.pointerInfo
        val x = mapping?.first?.map(pointer.ptHimetricLocationRaw.x.toFloat()) ?: pointer.ptPixelLocation.x.toFloat()
        val y = mapping?.second?.map(pointer.ptHimetricLocationRaw.y.toFloat()) ?: pointer.ptPixelLocation.y.toFloat()
        val contact = pointer.pointerFlags and POINTER_FLAG_INCONTACT != 0
        val phase = penPhase(contact, inContact, left)
        inContact = contact
        return PointerSample(
            x = x - origin.x,
            y = y - origin.y,
            phase = phase,
            kind = if (info.penFlags and (PEN_FLAG_ERASER or PEN_FLAG_INVERTED) != 0) PointerKind.Eraser else PointerKind.Pen,
            pressure = if (info.penMask and PEN_MASK_PRESSURE != 0) info.pressure / MAX_PRESSURE else null,
            tiltX = if (info.penMask and PEN_MASK_TILT_X != 0) info.tiltX.toFloat() else null,
            tiltY = if (info.penMask and PEN_MASK_TILT_Y != 0) info.tiltY.toFloat() else null,
            barrel = info.penFlags and PEN_FLAG_BARREL != 0,
            timeNanos = (pointer.performanceCount.toDouble() * 1e9 / counterFrequency).toLong(),
            coalesced = coalesced,
        )
    }

    /** Maps the pen's HIMETRIC device coordinates to sub-pixel screen coordinates. */
    private fun deviceMapping(device: Pointer?): Pair<AxisMapping, AxisMapping>? {
        val source = WinDef.RECT()
        val display = WinDef.RECT()
        if (!api.GetPointerDeviceRects(device, source, display)) return null
        if (source.right == source.left || source.bottom == source.top) return null
        return AxisMapping(source.left.toFloat(), source.right.toFloat(), display.left.toFloat(), display.right.toFloat()) to
            AxisMapping(source.top.toFloat(), source.bottom.toFloat(), display.top.toFloat(), display.bottom.toFloat())
    }
}
