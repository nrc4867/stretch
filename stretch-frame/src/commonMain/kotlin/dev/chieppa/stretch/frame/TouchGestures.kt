package dev.chieppa.stretch.frame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.atan2

/** Fingers closer than this to the centre of a gesture, in pixels, have no meaningful angle and are left out of a twist. */
private const val TWIST_MIN_RADIUS = 1f

private const val DEGREES_PER_HALF_TURN = 180f
private const val DEGREES_PER_TURN = 360f

/**
 * Two or more fingers on a touch screen moving the view: [onPan] gets how far their centre moved, [onZoom] the factor
 * their spread grew by (above 1 when spreading apart) and [onTwist] how many degrees they turned clockwise, each with
 * the gesture's centre as the focus, in this element's pixels. The fingers' events are consumed before anything inside
 * sees them, so a tool that took the first finger lets go when the second lands. One finger, the pen, the mouse and
 * fingers a modifier further out already consumed are left alone.
 */
fun Modifier.twoFingerGestures(
    onPan: (Offset) -> Unit,
    onZoom: (factor: Float, focus: Offset) -> Unit,
    onTwist: (degrees: Float, focus: Offset) -> Unit = { _, _ -> },
): Modifier = composed {
    val pan by rememberUpdatedState(onPan)
    val zoom by rememberUpdatedState(onZoom)
    val twist by rememberUpdatedState(onTwist)
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val touches = event.changes.filter { it.type == PointerType.Touch && it.pressed && !it.isConsumed }
                if (touches.size < 2) continue
                val moving = touches.filter { it.previousPressed }
                if (moving.size >= 2) {
                    val before = moving.map { it.previousPosition }
                    val after = moving.map { it.position }
                    val from = centroid(before)
                    val to = centroid(after)
                    if (to != from) pan(to - from)
                    val spreadBefore = spread(before, from)
                    if (spreadBefore > 0f) {
                        val factor = spread(after, to) / spreadBefore
                        if (factor != 1f) zoom(factor, to)
                    }
                    val turned = twistDegrees(before, after)
                    if (turned != 0f) twist(turned, to)
                }
                touches.forEach { it.consume() }
            }
        }
    }
}

/** The mean of [points]. */
internal fun centroid(points: List<Offset>): Offset = points.fold(Offset.Zero) { sum, p -> sum + p } / points.size.toFloat()

/** The mean distance of [points] from [centre]. */
internal fun spread(points: List<Offset>, centre: Offset): Float =
    points.sumOf { (it - centre).getDistance().toDouble() }.toFloat() / points.size

/**
 * How far fingers that moved from [before] to [after] (the same fingers, in order) turned around their centre, in
 * degrees: the mean change in each finger's angle, clockwise on screen. Fingers at the centre are left out.
 */
internal fun twistDegrees(before: List<Offset>, after: List<Offset>): Float {
    val from = centroid(before)
    val to = centroid(after)
    val turns = before.indices.mapNotNull { i ->
        val a = before[i] - from
        val b = after[i] - to
        if (a.getDistance() < TWIST_MIN_RADIUS || b.getDistance() < TWIST_MIN_RADIUS) null else wrapDegrees(degreesOf(b) - degreesOf(a))
    }
    return if (turns.isEmpty()) 0f else turns.sum() / turns.size
}

/** The direction of [d] in degrees, clockwise from the +x axis in screen coordinates (y down). */
private fun degreesOf(d: Offset): Float = atan2(d.y, d.x) * DEGREES_PER_HALF_TURN / PI.toFloat()

/** [degrees] brought into -180..180, so a finger crossing the ±180° line counts its short way round. */
private fun wrapDegrees(degrees: Float): Float {
    val r = ((degrees + DEGREES_PER_HALF_TURN) % DEGREES_PER_TURN + DEGREES_PER_TURN) % DEGREES_PER_TURN
    return r - DEGREES_PER_HALF_TURN
}
