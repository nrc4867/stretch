package dev.chieppa.stretch.web

import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import org.cef.browser.CefBrowser
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import kotlin.math.roundToInt

/** Passes the mouse over this box to the browser [browser] gives, at the page's coordinates, and asks for focus on a press. */
internal fun Modifier.browserPointerInput(density: Density, browser: () -> CefBrowser?, onPress: () -> Unit): Modifier =
    pointerInput(density) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val page = browser() ?: continue
                val awt = event.awtEventOrNull as? MouseEvent ?: continue
                val at = event.changes.firstOrNull()?.position ?: continue
                val x = (at.x / density.density).roundToInt()
                val y = (at.y / density.density).roundToInt()
                if (event.type == PointerEventType.Press) onPress()
                if (awt is MouseWheelEvent) page.sendMouseWheelEvent(wheelAt(awt, x, y)) else page.sendMouseEvent(mouseAt(awt, x, y))
                event.changes.forEach { it.consume() }
            }
        }
    }

private fun mouseAt(event: MouseEvent, x: Int, y: Int) =
    MouseEvent(event.component, event.id, event.`when`, event.modifiersEx, x, y, event.xOnScreen, event.yOnScreen, event.clickCount, event.isPopupTrigger, event.button)

private fun wheelAt(event: MouseWheelEvent, x: Int, y: Int) = MouseWheelEvent(
    event.component, event.id, event.`when`, event.modifiersEx, x, y, event.xOnScreen, event.yOnScreen, event.clickCount, event.isPopupTrigger,
    event.scrollType, event.scrollAmount, event.wheelRotation, event.preciseWheelRotation,
)

/**
 * Passes a key to [browser], whose own code picks the OS's key codes out of the AWT event. The page keeps what it is
 * typed; shortcuts with the platform's modifiers also carry on up to the app.
 */
internal fun forwardKey(event: KeyEvent, browser: CefBrowser?): Boolean {
    val page = browser ?: return false
    val awt = event.awtEventOrNull ?: return false
    page.sendKeyEvent(awt)
    return !(event.isCtrlPressed || event.isMetaPressed)
}
