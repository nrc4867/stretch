package dev.chieppa.stretch.frame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.ruler_horizontal
import dev.chieppa.stretch.frame.generated.ruler_vertical
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.formatNumber
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** How close to a guide or handle the pointer must be to grab it, in pixels. */
internal const val GRAB = 5f

/** How far past a guide, ruler or handle a fingertip still grabs it; a finger covers far more than a pointer. */
internal val FingerReach = 14.dp

/** A marquee drag in progress: the rectangle being dragged, in the view's pixels, or null when there is none. */
@Stable
class MarqueeState {
    var box: Rect? by mutableStateOf(null)
        internal set
}

@Composable
fun rememberMarqueeState(): MarqueeState = remember { MarqueeState() }

/** How strongly a marquee tints what it covers. */
private const val MARQUEE_FILL_ALPHA = 0.1f

/**
 * Drag a rectangle over empty scene to select what it touches: [onMarquee] gets the rectangle in scene units, and
 * whether Shift, Ctrl or Cmd was held to add to the selection. Presses something else used, such as
 * [transformHandles], are left alone, and a press that does not move far is left as a click. Draws the rectangle
 * while it is dragged.
 */
fun Modifier.marquee(camera: ViewportCamera, onMarquee: (scene: Rect, additive: Boolean) -> Unit): Modifier = composed {
    val c = Stretch.colors
    val state = rememberMarqueeState()
    marquee(state, camera, onMarquee).drawWithContent {
        drawContent()
        state.box?.let { r ->
            drawRect(c.ringSelected.copy(alpha = MARQUEE_FILL_ALPHA), r.topLeft, r.size)
            drawRect(
                c.ringSelected,
                r.topLeft,
                r.size,
                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
            )
        }
    }
}

/**
 * [marquee]'s pointer handling alone: the rectangle being dragged goes in [state] for something else to draw, such as
 * a renderer through [marqueeOverlay].
 */
fun Modifier.marquee(state: MarqueeState, camera: ViewportCamera, onMarquee: (scene: Rect, additive: Boolean) -> Unit): Modifier = composed {
    val report by rememberUpdatedState(onMarquee)
    pointerInput(camera, state) {
        awaitEachGesture {
            val down = awaitFirstDown()
            if (!currentEvent.buttons.isPrimaryPressed && down.type == PointerType.Mouse) return@awaitEachGesture
            val start = down.position
            var dragged = false
            var additive = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.isConsumed) {
                    state.box = null
                    return@awaitEachGesture
                }
                additive = event.keyboardModifiers.isShiftPressed || event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
                if (!dragged && (change.position - start).getDistance() > viewConfiguration.touchSlop) dragged = true
                if (dragged) {
                    state.box = Rect(start, change.position).normalized()
                    change.consume()
                }
                if (!change.pressed) break
            }
            state.box?.let { report(camera.toScene(it), additive) }
            state.box = null
        }
    }
}

/** A marquee rectangle [box], in the view's pixels, for a renderer to draw over its scene: tinted, with a [color] edge. */
fun marqueeOverlay(box: Rect?, color: Color, width: Float = 1f): ViewportOverlay = buildOverlay {
    if (box == null) return@buildOverlay
    rect(box, color.copy(alpha = MARQUEE_FILL_ALPHA))
    outline(box, color, width)
}

private fun Rect.normalized() = Rect(minOf(left, right), minOf(top, bottom), maxOf(left, right), maxOf(top, bottom))

/** A selection's box in scene units: its unrotated [bounds] and its [rotation] in degrees about their centre. */
data class BoxTransform(val bounds: Rect, val rotation: Float = 0f)

/** The size of a handle (half its width) and how far above the box the rotate handle sits. */
private val HandleSize = 4.dp
private val RotateStem = 22.dp

/** The handles around a box, by index: corners and edge midpoints clockwise from the top-left, then rotate, then the inside. */
private const val ROTATE = 8
private const val INSIDE = 9

/** Unit positions of the eight resize handles in the box, 0..1 on each axis. */
private val handleSpots = listOf(
    Offset(0f, 0f), Offset(0.5f, 0f), Offset(1f, 0f), Offset(1f, 0.5f),
    Offset(1f, 1f), Offset(0.5f, 1f), Offset(0f, 1f), Offset(0f, 0.5f),
)

/**
 * Selection handles for [transform]: drag inside the box to move it, a corner or edge handle to resize it from the
 * opposite side (Shift keeps a corner's proportions), and the round handle above it to rotate (Shift snaps to 15°).
 * Each step reports [onTransform]; [onTransformFinished] marks the end of the drag, where an app commits one undo
 * step. Put it after [marquee] on the same element so handles win over starting a marquee. Null draws nothing.
 */
fun Modifier.transformHandles(
    transform: BoxTransform?,
    camera: ViewportCamera,
    onTransform: (BoxTransform) -> Unit,
    onTransformFinished: () -> Unit = {},
): Modifier = composed {
    val c = Stretch.colors
    transformHandleInput(transform, camera, onTransform, onTransformFinished).drawWithContent {
        drawContent()
        val t = transform ?: return@drawWithContent
        drawHandles(t, camera, c.ringSelected, c.surfaceFloat, HandleSize.toPx(), RotateStem.toPx())
    }
}

/**
 * [transformHandles]' pointer handling and pointer shapes alone, for handles something else draws, such as a renderer
 * through [handlesOverlay].
 */
fun Modifier.transformHandleInput(
    transform: BoxTransform?,
    camera: ViewportCamera,
    onTransform: (BoxTransform) -> Unit,
    onTransformFinished: () -> Unit = {},
): Modifier = composed {
    val current by rememberUpdatedState(transform)
    val report by rememberUpdatedState(onTransform)
    val finished by rememberUpdatedState(onTransformFinished)
    var hover by remember { mutableStateOf(-1) }
    val icon = cursorFor(hover, transform?.rotation ?: 0f)
    pointerInput(camera) {
        val handle = HandleSize.toPx()
        val stem = RotateStem.toPx()
        val fingerReach = FingerReach.toPx()
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val t = current
                val change = event.changes.firstOrNull() ?: continue
                if (t == null) {
                    hover = -1
                    continue
                }
                val reach = if (change.type == PointerType.Touch) fingerReach else GRAB
                val part = partAt(t, camera, change.position, handle, stem, reach)
                if (event.type != PointerEventType.Press) {
                    if (!change.pressed) hover = part
                    continue
                }
                if (part < 0 || change.isConsumed) continue
                change.consume()
                drag(t, part, camera, change) { report(it) }
                finished()
            }
        }
    }.then(if (icon != null) Modifier.pointerHoverIcon(icon) else Modifier)
}

/**
 * This box's handles for a renderer to draw over its scene, in [camera]'s view pixels at [density]: the box's
 * outline and the rotate handle's stem in [lineColor], and each handle as a [fillColor] dot ringed in [lineColor]. Dots stay round
 * however the box turns, where [transformHandles] draws square ones.
 */
fun BoxTransform.handlesOverlay(camera: ViewportCamera, lineColor: Color, fillColor: Color, density: Density): ViewportOverlay = buildOverlay {
    val handle = with(density) { HandleSize.toPx() }
    val stem = with(density) { RotateStem.toPx() }
    val stroke = density.density
    val screen = camera.toScreen(bounds)
    fun turned(p: Offset) = rotateAround(p, screen.center, rotation)
    val corners = listOf(screen.topLeft, screen.topRight, screen.bottomRight, screen.bottomLeft).map(::turned)
    corners.forEachIndexed { i, p -> line(p, corners[(i + 1) % corners.size], lineColor, stroke) }
    val top = Offset(screen.center.x, screen.top)
    line(turned(top), turned(top - Offset(0f, stem - handle)), lineColor, stroke)
    val spots = handleSpots.map { s -> Offset(screen.left + s.x * screen.width, screen.top + s.y * screen.height) } + (top - Offset(0f, stem))
    for (spot in spots) {
        dot(turned(spot), handle + 1f + stroke, lineColor)
        dot(turned(spot), handle + 1f, fillColor)
    }
}

/** Follows one drag of [part] of the box, from [down], until the pointer lifts. */
private suspend fun AwaitPointerEventScope.drag(
    start: BoxTransform,
    part: Int,
    camera: ViewportCamera,
    down: PointerInputChange,
    onTransform: (BoxTransform) -> Unit,
) {
    val from = camera.toScene(down.position)
    val centre = start.bounds.center
    val startAngle = angleOf(from - centre)
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == down.id } ?: break
        if (change.isConsumed) {
            // Something else took the gesture, such as a second finger panning the view: put the box back.
            onTransform(start)
            break
        }
        if (!change.pressed) break
        if (change.positionChange() == Offset.Zero) continue
        change.consume()
        val p = camera.toScene(change.position)
        val shift = event.keyboardModifiers.isShiftPressed
        onTransform(
            when (part) {
                INSIDE -> start.copy(bounds = start.bounds.translate(p - from))
                ROTATE -> {
                    var angle = start.rotation + angleOf(p - centre) - startAngle
                    if (shift) angle = (angle / 15f).roundToInt() * 15f
                    start.copy(rotation = ((angle % 360f) + 360f) % 360f)
                }
                else -> resize(start, part, p, shift)
            },
        )
    }
}

/** The box after dragging handle [part] to scene point [p], with the opposite handle kept where it was. */
internal fun resize(start: BoxTransform, part: Int, p: Offset, keepRatio: Boolean): BoxTransform {
    val b = start.bounds
    val centre = b.center
    val local = rotateAround(p, centre, -start.rotation)
    val spot = handleSpots[part]
    var x0 = b.left + (1 - spot.x) * b.width
    var y0 = b.top + (1 - spot.y) * b.height
    var x1 = local.x
    var y1 = local.y
    // An edge handle moves one side; the box keeps its span on the other axis.
    if (spot.x == 0.5f) { x0 = b.left; x1 = b.right }
    if (spot.y == 0.5f) { y0 = b.top; y1 = b.bottom }
    if (keepRatio && spot.x != 0.5f && spot.y != 0.5f && b.width > 0f && b.height > 0f) {
        val scale = max(abs(x1 - x0) / b.width, abs(y1 - y0) / b.height)
        x1 = x0 + (if (x1 >= x0) 1 else -1) * b.width * scale
        y1 = y0 + (if (y1 >= y0) 1 else -1) * b.height * scale
    }
    val sized = Rect(Offset(x0, y0), Offset(x1, y1)).normalized()
    // The new box was worked out unrotated about the old centre; turn its centre back so the anchor stays put.
    val newCentre = rotateAround(sized.center, centre, start.rotation)
    return start.copy(bounds = Rect(newCentre - Offset(sized.width / 2, sized.height / 2), sized.size))
}

private fun rotateAround(p: Offset, centre: Offset, degrees: Float): Offset {
    if (degrees == 0f) return p
    val r = degrees * PI.toFloat() / 180f
    val d = p - centre
    return centre + Offset(d.x * cos(r) - d.y * sin(r), d.x * sin(r) + d.y * cos(r))
}

/** Degrees clockwise from straight up. */
private fun angleOf(d: Offset): Float = atan2(d.x, -d.y) * 180f / PI.toFloat()

/** Which part of the box is at screen point [at], or -1 for none. */
private fun partAt(t: BoxTransform, camera: ViewportCamera, at: Offset, handle: Float, stem: Float, reach: Float): Int {
    val screen = camera.toScreen(t.bounds)
    val local = rotateAround(at, screen.center, -t.rotation)
    if ((local - Offset(screen.center.x, screen.top - stem)).getDistance() <= handle + reach) return ROTATE
    handleSpots.forEachIndexed { i, s ->
        val spot = Offset(screen.left + s.x * screen.width, screen.top + s.y * screen.height)
        if (abs(local.x - spot.x) <= handle + reach && abs(local.y - spot.y) <= handle + reach) return i
    }
    return if (screen.contains(local)) INSIDE else -1
}

private fun DrawScope.drawHandles(t: BoxTransform, camera: ViewportCamera, line: Color, fill: Color, handle: Float, stem: Float) {
    val screen = camera.toScreen(t.bounds)
    rotate(t.rotation, screen.center) {
        val stroke = Stroke(1.dp.toPx())
        drawRect(line, screen.topLeft, screen.size, style = stroke)
        val top = Offset(screen.center.x, screen.top)
        drawLine(line, top, top - Offset(0f, stem - handle), 1.dp.toPx())
        drawCircle(fill, handle + 1f, top - Offset(0f, stem))
        drawCircle(line, handle + 1f, top - Offset(0f, stem), style = stroke)
        for (s in handleSpots) {
            val spot = Offset(screen.left + s.x * screen.width, screen.top + s.y * screen.height)
            val box = Size(handle * 2, handle * 2)
            drawRect(fill, spot - Offset(handle, handle), box)
            drawRect(line, spot - Offset(handle, handle), box, style = stroke)
        }
    }
}

/** The pointer for [part]: a resize arrow along the handle's direction once the box's rotation is applied. */
private fun cursorFor(part: Int, rotation: Float): PointerIcon? = when (part) {
    -1 -> null
    INSIDE -> moveAround
    ROTATE -> PointerIcon.Hand
    else -> {
        val s = handleSpots[part]
        val angle = angleOf(Offset(s.x - 0.5f, s.y - 0.5f)) + rotation
        when ((((angle + 22.5f) % 180f + 180f) % 180f / 45f).toInt()) {
            0 -> resizeUpDown
            1 -> resizeDiagonalUp
            2 -> resizeSideways
            else -> resizeDiagonalDown
        }
    }
}
