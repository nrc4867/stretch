package dev.chieppa.stretch.web

import java.awt.event.MouseWheelEvent
import kotlin.math.roundToInt

/** Points a line of wheel travel scrolls a page: what Chromium takes for a line on Windows and Linux. */
private const val LINE_POINTS = 40.0

/** Points macOS's finer wheel units scroll, as Compose scrolls its own lists there. */
private const val MAC_UNIT_POINTS = 10.0

private val osName = System.getProperty("os.name").orEmpty()
private val onWindows = osName.startsWith("Windows")
private val onMac = osName.startsWith("Mac")

/**
 * Turns AWT wheel events into the points a page scrolls by. JCEF passes an event's own units to Chromium, which reads
 * them as points, flipped on Windows only, so this hands it points already signed for each OS. Fractions carry over,
 * so a trackpad's small steps add up.
 */
internal class WheelPixels {
    private var carried = 0.0

    /** The event to send for [event] at the page's [x] and [y], or null while the travel is under a point. */
    fun eventFor(event: MouseWheelEvent, x: Int, y: Int): MouseWheelEvent? {
        val unit = if (onMac) MAC_UNIT_POINTS else LINE_POINTS
        carried += event.preciseWheelRotation * event.scrollAmount * unit
        val points = carried.roundToInt()
        if (points == 0) return null
        carried -= points
        // Shift turns the wheel sideways, which JCEF never flips.
        val sideways = event.modifiersEx and MouseWheelEvent.SHIFT_DOWN_MASK != 0
        val rotation = if (onWindows && !sideways) points else -points
        return MouseWheelEvent(
            event.component, event.id, event.`when`, event.modifiersEx, x, y, event.xOnScreen, event.yOnScreen, event.clickCount,
            event.isPopupTrigger, MouseWheelEvent.WHEEL_BLOCK_SCROLL, 1, rotation, rotation.toDouble(),
        )
    }
}
