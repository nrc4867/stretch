package dev.chieppa.stretch.gpu

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.exp

/** How much one wheel notch moves the camera in or out. */
private const val WHEEL_DOLLY = 0.15f

/**
 * Moves [camera] the way 3D tools do: dragging with the middle button orbits around the target, with Shift held it
 * pans, and the wheel moves in and out. The left button, pens and touch are left to the app's tools.
 */
fun Modifier.orbitNavigation(camera: OrbitCamera): Modifier = pointerInput(camera) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            when {
                event.type == PointerEventType.Scroll -> {
                    val change = event.changes.first()
                    camera.dollyBy(exp(-change.scrollDelta.y * WHEEL_DOLLY))
                    change.consume()
                }
                event.buttons.isTertiaryPressed && event.changes.any { it.type == PointerType.Mouse } -> {
                    event.changes.forEach { change ->
                        if (change.previousPressed) {
                            val delta = change.position - change.previousPosition
                            if (event.keyboardModifiers.isShiftPressed) {
                                camera.panBy(delta.x, delta.y, size.height.toFloat())
                            } else {
                                camera.orbitBy(delta.x, delta.y)
                            }
                        }
                        change.consume()
                    }
                }
            }
        }
    }
}
