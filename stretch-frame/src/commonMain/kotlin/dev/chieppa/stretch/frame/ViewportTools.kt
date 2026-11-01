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

/** A guide line across the scene: at x = [position] when [vertical], at y = [position] otherwise. */
data class Guide(val vertical: Boolean, val position: Float)

/** How thick the viewport's rulers are. */
val RulerSize = 20.dp

/** How close to a guide or handle the pointer must be to grab it, in pixels. */
private const val GRAB = 5f

/** The scene distance between labelled ruler ticks at [zoom]: 1, 2 or 5 times a power of ten, at least 64px apart. */
fun rulerStep(zoom: Float): Float {
    val raw = 64f / zoom
    val power = 10f.pow(floor(log10(raw)))
    return listOf(1f, 2f, 5f, 10f).first { it * power >= raw - 1e-6f } * power
}

/**
 * Rulers on the viewport's top and left edges, in scene units at [camera]'s zoom, and the [guides] across it.
 * Dragging out of a ruler places a guide; dragging a guide moves it, and dropping it back on its ruler removes it.
 * Each change comes back through [onGuidesChange] when the drag ends. [Viewport] draws these when given rulers.
 */
@Composable
internal fun RulersAndGuides(camera: ViewportCamera, guides: List<Guide>, onGuidesChange: ((List<Guide>) -> Unit)?) {
    // The scene's axes run left to right in every language, so the rulers do too.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Rulers(camera, guides, onGuidesChange) }
}

@Composable
private fun Rulers(camera: ViewportCamera, guides: List<Guide>, onGuidesChange: ((List<Guide>) -> Unit)?) {
    val c = Stretch.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = Stretch.type.valueSm.copy(color = c.inkSubtle)
    var pending by remember { mutableStateOf<Guide?>(null) }
    var moving by remember { mutableStateOf<Int?>(null) }
    val current by rememberUpdatedState(guides)
    val change by rememberUpdatedState(onGuidesChange)

    fun finish(guide: Guide, ruler: Float, index: Int?) {
        val gone = guide.position.let { if (guide.vertical) camera.toScreen(Offset(it, 0f)).x else camera.toScreen(Offset(0f, it)).y } < ruler
        val list = current.toMutableList()
        if (index != null) list.removeAt(index)
        if (!gone) list.add(index ?: list.size, guide)
        change?.invoke(list)
    }

    Box(
        Modifier.fillMaxSize().pointerInput(camera) {
            if (onGuidesChange == null) return@pointerInput
            val ruler = RulerSize.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (down.position.x < ruler || down.position.y < ruler) return@awaitEachGesture
                val index = current.indexOfFirst { g ->
                    val at = if (g.vertical) camera.toScreen(Offset(g.position, 0f)).x else camera.toScreen(Offset(0f, g.position)).y
                    abs((if (g.vertical) down.position.x else down.position.y) - at) <= GRAB
                }
                if (index < 0) return@awaitEachGesture
                down.consume()
                moving = index
                val guide = current[index]
                pending = guide
                dragUntilUp(PointerEventPass.Initial) { p ->
                    val scene = camera.toScene(p)
                    pending = guide.copy(position = if (guide.vertical) scene.x else scene.y)
                }
                pending?.let { finish(it, ruler, index) }
                pending = null
                moving = null
            }
        },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val shown = current.filterIndexed { i, _ -> i != moving } + listOfNotNull(pending)
            for (g in shown) {
                if (g.vertical) {
                    val x = camera.toScreen(Offset(g.position, 0f)).x
                    drawLine(c.accent, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                } else {
                    val y = camera.toScreen(Offset(0f, g.position)).y
                    drawLine(c.accent, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
            }
        }
        for (vertical in listOf(false, true)) {
            val description = stringResource(if (vertical) Res.string.ruler_vertical else Res.string.ruler_horizontal)
            Canvas(
                (if (vertical) Modifier.width(RulerSize).fillMaxHeight() else Modifier.height(RulerSize).fillMaxWidth())
                    .clipToBounds()
                    .semantics { contentDescription = description }
                    .pointerInput(camera, vertical) {
                        if (onGuidesChange == null) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            dragUntilUp(PointerEventPass.Main) { p ->
                                val scene = camera.toScene(p)
                                pending = Guide(vertical, if (vertical) scene.x else scene.y)
                            }
                            pending?.let { finish(it, RulerSize.toPx(), null) }
                            pending = null
                        }
                    },
            ) {
                drawRect(c.surfacePanel)
                val step = rulerStep(camera.zoom)
                val minor = step / if (abs(step / 10f.pow(floor(log10(step))) - 2f) < 0.01f) 4 else 5
                val length = if (vertical) size.height else size.width
                val from = camera.toScene(Offset.Zero).let { if (vertical) it.y else it.x }
                val to = camera.toScene(Offset(length, length)).let { if (vertical) it.y else it.x }
                var v = floor(from / minor) * minor
                val thickness = if (vertical) size.width else size.height
                while (v <= to) {
                    val at = camera.toScreen(Offset(v, v)).let { if (vertical) it.y else it.x }
                    val major = abs(v / step - (v / step).roundToInt()) < 1e-3f
                    val tick = if (major) thickness * 0.55f else thickness * 0.25f
                    if (vertical) {
                        drawLine(c.lineControl, Offset(thickness - tick, at), Offset(thickness, at), 1f)
                    } else {
                        drawLine(c.lineControl, Offset(at, thickness - tick), Offset(at, thickness), 1f)
                    }
                    if (major) {
                        val label = measurer.measure(formatNumber(v), labelStyle)
                        // Labels that would run under the corner square are left out rather than cut.
                        if (vertical && at - label.size.width < thickness || !vertical && at < thickness) {
                            v += minor
                            continue
                        }
                        if (vertical) {
                            rotate(-90f, Offset(2f, at - 3f)) { drawText(label, topLeft = Offset(2f, at - 3f)) }
                        } else {
                            drawText(label, topLeft = Offset(at + 3f, 1f))
                        }
                    }
                    v += minor
                }
                if (vertical) {
                    drawLine(c.linePanel, Offset(size.width - 0.5f, 0f), Offset(size.width - 0.5f, size.height), 1f)
                } else {
                    drawLine(c.linePanel, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1f)
                }
            }
        }
        Canvas(Modifier.size(RulerSize)) { drawRect(c.surfacePanel) }
    }
}

/** Follows the pressed pointer until it lifts, reporting its position in this element's coordinates. */
private suspend fun AwaitPointerEventScope.dragUntilUp(pass: PointerEventPass, onMove: (Offset) -> Unit) {
    while (true) {
        val event = awaitPointerEvent(pass)
        val change = event.changes.firstOrNull() ?: break
        if (!change.pressed) break
        onMove(change.position)
        change.consume()
    }
}

/**
 * Drag a rectangle over empty scene to select what it touches: [onMarquee] gets the rectangle in scene units, and
 * whether Shift, Ctrl or Cmd was held to add to the selection. Presses something else used, such as
 * [transformHandles], are left alone, and a press that does not move far is left as a click.
 */
fun Modifier.marquee(camera: ViewportCamera, onMarquee: (scene: Rect, additive: Boolean) -> Unit): Modifier = composed {
    val c = Stretch.colors
    val report by rememberUpdatedState(onMarquee)
    var box by remember { mutableStateOf<Rect?>(null) }
    pointerInput(camera) {
        awaitEachGesture {
            val down = awaitFirstDown()
            if (!currentEvent.buttons.isPrimaryPressed && down.type == androidx.compose.ui.input.pointer.PointerType.Mouse) return@awaitEachGesture
            val start = down.position
            var dragged = false
            var additive = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.isConsumed) {
                    box = null
                    return@awaitEachGesture
                }
                additive = event.keyboardModifiers.isShiftPressed || event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
                if (!dragged && (change.position - start).getDistance() > viewConfiguration.touchSlop) dragged = true
                if (dragged) {
                    box = Rect(start, change.position).normalized()
                    change.consume()
                }
                if (!change.pressed) break
            }
            box?.let { report(camera.toScene(it), additive) }
            box = null
        }
    }.drawWithContent {
        drawContent()
        box?.let { r ->
            drawRect(c.ringSelected.copy(alpha = 0.1f), r.topLeft, r.size)
            drawRect(
                c.ringSelected,
                r.topLeft,
                r.size,
                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
            )
        }
    }
}

private fun Rect.normalized() = Rect(minOf(left, right), minOf(top, bottom), maxOf(left, right), maxOf(top, bottom))

/** A selection's box in scene units: its unrotated [bounds] and its [rotation] in degrees about their centre. */
data class BoxTransform(val bounds: Rect, val rotation: Float = 0f)

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
    val current by rememberUpdatedState(transform)
    val report by rememberUpdatedState(onTransform)
    val finished by rememberUpdatedState(onTransformFinished)
    var hover by remember { mutableStateOf(-1) }
    val icon = cursorFor(hover, transform?.rotation ?: 0f)
    pointerInput(camera) {
        val handle = 4.dp.toPx()
        val stem = 22.dp.toPx()
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val t = current
                val change = event.changes.firstOrNull() ?: continue
                if (t == null) {
                    hover = -1
                    continue
                }
                val part = partAt(t, camera, change.position, handle, stem)
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
    }.then(if (icon != null) Modifier.pointerHoverIcon(icon) else Modifier).drawWithContent {
        drawContent()
        val t = transform ?: return@drawWithContent
        drawHandles(t, camera, c.ringSelected, c.surfaceFloat, 4.dp.toPx(), 22.dp.toPx())
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
private fun partAt(t: BoxTransform, camera: ViewportCamera, at: Offset, handle: Float, stem: Float): Int {
    val screen = camera.toScreen(t.bounds)
    val local = rotateAround(at, screen.center, -t.rotation)
    if ((local - Offset(screen.center.x, screen.top - stem)).getDistance() <= handle + GRAB) return ROTATE
    handleSpots.forEachIndexed { i, s ->
        val spot = Offset(screen.left + s.x * screen.width, screen.top + s.y * screen.height)
        if (abs(local.x - spot.x) <= handle + GRAB && abs(local.y - spot.y) <= handle + GRAB) return i
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
