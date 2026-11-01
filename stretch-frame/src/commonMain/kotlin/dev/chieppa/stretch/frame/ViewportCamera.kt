package dev.chieppa.stretch.frame

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Where the viewport looks: the scene point at the view's top-left corner is [offset] away from the view's origin
 * and one scene unit is [zoom] pixels. Scene coordinates are the app's own; the camera converts both ways with
 * [toScreen] and [toScene]. [Modifier.viewportNavigation] moves it with the wheel, the middle button and two fingers,
 * and the [ViewNavigator] reads and sets [zoom].
 */
@Stable
class ViewportCamera(zoom: Float = 1f, offset: Offset = Offset.Zero, val zoomRange: ClosedFloatingPointRange<Float> = 0.1f..8f) {
    var zoom by mutableFloatStateOf(zoom.coerceIn(zoomRange))
        private set

    /** The screen position of the scene's origin, in pixels from the view's top-left. */
    var offset by mutableStateOf(offset)
        private set

    /** The view's size in pixels, kept up to date by [Modifier.viewportNavigation]. */
    var viewSize by mutableStateOf(Size.Zero)
        internal set

    fun toScreen(scene: Offset): Offset = scene * zoom + offset

    fun toScene(screen: Offset): Offset = (screen - offset) / zoom

    fun toScreen(scene: Rect): Rect = Rect(toScreen(scene.topLeft), toScreen(scene.bottomRight))

    fun toScene(screen: Rect): Rect = Rect(toScene(screen.topLeft), toScene(screen.bottomRight))

    /** The part of the scene in view. */
    val visible: Rect get() = toScene(Rect(Offset.Zero, viewSize))

    fun panBy(delta: Offset) {
        offset += delta
    }

    /** Zooms to [value], keeping the scene point under [focus] (by default the view's centre) where it is. */
    fun zoomTo(value: Float, focus: Offset = viewSize.center) {
        val anchor = toScene(focus)
        zoom = value.coerceIn(zoomRange)
        offset = focus - anchor * zoom
    }

    fun zoomBy(factor: Float, focus: Offset = viewSize.center) = zoomTo(zoom * factor, focus)

    /** Zooms and pans so [scene] fills the view, [margin] pixels in from its edges. */
    fun fit(scene: Rect, margin: Float = 48f) {
        if (viewSize.width <= 0f || viewSize.height <= 0f || scene.width <= 0f || scene.height <= 0f) return
        val room = Size(max(1f, viewSize.width - 2 * margin), max(1f, viewSize.height - 2 * margin))
        zoom = min(room.width / scene.width, room.height / scene.height).coerceIn(zoomRange)
        offset = viewSize.center - scene.center * zoom
    }
}

private val Size.center get() = Offset(width / 2, height / 2)

/** How far one wheel notch pans the view, in pixels. */
private const val WHEEL_STEP = 48f

/** How much one wheel notch with Ctrl or Cmd zooms the view. */
private const val WHEEL_ZOOM = 0.15f

/**
 * Moves [camera] the ways every canvas app does: the wheel pans (with Shift, sideways) and Ctrl or Cmd with the
 * wheel zooms at the pointer, as does a touchpad pinch; the middle button drags the view; two fingers on a touch
 * screen pan and pinch-zoom together (see [twoFingerGestures]). One finger, the pen and the left button are left to
 * the app's tools. Events a modifier further out already consumed are left alone.
 */
fun Modifier.viewportNavigation(camera: ViewportCamera): Modifier =
    onSizeChanged { camera.viewSize = it.toSize() }
        .twoFingerGestures(onPan = camera::panBy, onZoom = camera::zoomBy)
        .pointerInput(camera) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    when {
                        // Something outside the viewport, such as a 3D camera's navigation, took the event.
                        event.changes.all { it.isConsumed } -> Unit
                        event.type == PointerEventType.Scroll -> {
                            val change = event.changes.first()
                            val delta = change.scrollDelta
                            if (event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed) {
                                camera.zoomBy(exp(-delta.y * WHEEL_ZOOM), change.position)
                            } else {
                                camera.panBy(-delta * WHEEL_STEP)
                            }
                            change.consume()
                        }
                        event.buttons.isTertiaryPressed && event.changes.any { it.type == PointerType.Mouse } -> {
                            event.changes.forEach { change ->
                                if (change.previousPressed) camera.panBy(change.position - change.previousPosition)
                                change.consume()
                            }
                        }
                    }
                }
            }
        }
