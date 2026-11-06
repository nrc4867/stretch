package dev.chieppa.stretch.frame

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow

/**
 * The parts of a title bar that are app controls. The rest of the bar belongs to the OS, which drags the window,
 * maximizes on double-click and opens the system menu there.
 */
internal class TitleBarRegions {
    private val controls = mutableMapOf<Any, Rect>()
    internal var origin = Offset.Zero

    fun isControl(pointInWindow: Offset) = controls.values.any { it.contains(pointInWindow) }

    internal fun put(key: Any, bounds: Rect) {
        controls[key] = bounds
    }

    /** Forgets the control under [key], once it is no longer placed in the bar. */
    internal fun remove(key: Any) {
        controls.remove(key)
    }
}

/** Marks this element, under [key], as a control in the title bar, so pointer input reaches it instead of the OS. */
internal fun Modifier.titleBarControl(regions: TitleBarRegions, key: Any): Modifier =
    onGloballyPositioned { regions.put(key, it.boundsInWindow()) }

/**
 * Reports, for every pointer event over the bar and before anything in it reacts, whether the pointer is over one of
 * its controls (true) or over bar the OS should handle (false).
 */
internal fun Modifier.titleBarHitTest(regions: TitleBarRegions, onHit: (control: Boolean) -> Unit): Modifier =
    onGloballyPositioned { regions.origin = it.positionInWindow() }
        .pointerInput(regions, onHit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Exit || event.type == PointerEventType.Scroll) continue
                    val position = event.changes.firstOrNull()?.position ?: continue
                    onHit(regions.isControl(regions.origin + position))
                }
            }
        }
