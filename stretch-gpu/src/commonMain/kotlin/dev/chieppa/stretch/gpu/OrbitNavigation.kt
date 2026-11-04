package dev.chieppa.stretch.gpu

import dev.chieppa.stretch.frame.wheelNotches
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import dev.chieppa.stretch.frame.twoFingerGestures
import kotlin.math.exp

/** How much one wheel notch moves the camera in or out. */
private const val WHEEL_DOLLY = 0.15f

/**
 * Moves [camera] the way 3D tools do: dragging with the middle button orbits around the target, with Shift held it
 * pans, and the wheel moves in and out, with or without Ctrl or Cmd (a touchpad pinch arrives as Ctrl with the wheel).
 * On a touch screen two fingers move it (see [twoFingerGestures]): moving together pans, pinching moves in and out,
 * and twisting turns the camera around the target's vertical axis. The left button, one finger and the pen are left
 * to the app's tools.
 */
fun Modifier.orbitNavigation(camera: OrbitCamera): Modifier = composed {
    val height = remember { mutableIntStateOf(0) }
    onSizeChanged { height.intValue = it.height }
        .twoFingerGestures(
            onPan = { delta -> camera.panBy(delta.x, delta.y, height.intValue.toFloat()) },
            onZoom = { factor, _ -> camera.dollyBy(factor) },
            onTwist = { degrees, _ -> camera.turnBy(degrees) },
        )
        .pointerInput(camera) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    when {
                        event.type == PointerEventType.Scroll -> {
                            val change = event.changes.first()
                            camera.dollyBy(exp(-change.wheelNotches.y * WHEEL_DOLLY))
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
}
