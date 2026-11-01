package dev.chieppa.stretch.frame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.ruler_horizontal
import dev.chieppa.stretch.frame.generated.ruler_vertical
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.formatNumber
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/** A guide line across the scene: at x = [position] when [vertical], at y = [position] otherwise. */
data class Guide(val vertical: Boolean, val position: Float)

/** How thick the viewport's rulers are. */
val RulerSize = 20.dp

/** The scene distance between labelled ruler ticks at [zoom]: 1, 2 or 5 times a power of ten, at least 64px apart. */
fun rulerStep(zoom: Float): Float {
    val raw = 64f / zoom
    val power = 10f.pow(floor(log10(raw)))
    return listOf(1f, 2f, 5f, 10f).first { it * power >= raw - 1e-6f } * power
}

/** What a press would pick up: the guide at [index], or a new guide out of a ruler when [index] is null. */
internal data class GuideGrab(val vertical: Boolean, val index: Int?)

/**
 * A viewport's guide drag in progress, shared by its pointer handling and whatever draws the guides. A [Viewport]
 * makes its own; an app whose renderer draws the guides holds one, passes it to the viewport, and draws [shown].
 */
@Stable
class GuideDragState {
    /** Where the dragged guide is now, or null when nothing is being dragged. */
    internal var pending by mutableStateOf<Guide?>(null)

    /** The index in the guide list of the guide being moved, or null when none is (or a new one is being placed). */
    internal var moving by mutableStateOf<Int?>(null)

    /** What the mouse is over and would pick up, for its pointer. */
    internal var hover by mutableStateOf<GuideGrab?>(null)

    /** Areas of the viewport's own controls, such as its toolbar, where guides and rulers never take the pointer. */
    internal var blocked: List<Rect> = emptyList()

    /** [guides] as they look right now: the one being dragged at the pointer, and a new one on its way out of a ruler. */
    fun shown(guides: List<Guide>): List<Guide> = guides.filterIndexed { i, _ -> i != moving } + listOfNotNull(pending)
}

@Composable
fun rememberGuideDragState(): GuideDragState = remember { GuideDragState() }

/**
 * [guides] as lines across [camera]'s view, for a renderer to draw over its scene, [width] pixels wide. Positions
 * are in the view's pixels, which for a [Viewport] with native content are the surface's.
 */
fun guidesOverlay(camera: ViewportCamera, guides: List<Guide>, color: Color, width: Float = 1f): ViewportOverlay = buildOverlay {
    val view = camera.viewSize
    for (g in guides) {
        val at = screenAt(camera, g)
        if (g.vertical) line(Offset(at, 0f), Offset(at, view.height), color, width) else line(Offset(0f, at), Offset(view.width, at), color, width)
    }
}

/** Where [guide] sits across the view, in screen pixels on its own axis. */
private fun screenAt(camera: ViewportCamera, guide: Guide): Float =
    if (guide.vertical) camera.toScreen(Offset(guide.position, 0f)).x else camera.toScreen(Offset(0f, guide.position)).y

/**
 * What a press at viewport point [at] picks up: the nearest guide within [guideReach], else a new guide from a ruler
 * when [at] is on it or within [rulerReach] of it. Null over the corner square, the [blocked] areas, or nothing.
 * [origin] is where [camera]'s view starts in the viewport: past the rulers when they form a band.
 */
internal fun grabAt(
    camera: ViewportCamera,
    guides: List<Guide>,
    at: Offset,
    ruler: Float,
    guideReach: Float,
    rulerReach: Float,
    blocked: List<Rect>,
    origin: Offset = Offset.Zero,
): GuideGrab? {
    if (blocked.any { it.contains(at) }) return null
    if (at.x < ruler && at.y < ruler) return null
    if (at.x >= ruler && at.y >= ruler) {
        val nearest = guides.indices
            .map { i -> i to abs((if (guides[i].vertical) at.x - origin.x else at.y - origin.y) - screenAt(camera, guides[i])) }
            .filter { (_, distance) -> distance <= guideReach }
            .minByOrNull { (_, distance) -> distance }
        if (nearest != null) return GuideGrab(guides[nearest.first].vertical, nearest.first)
    }
    val top = at.y < ruler + rulerReach
    val left = at.x < ruler + rulerReach
    return when {
        top && left -> GuideGrab(vertical = at.x - ruler < at.y - ruler, index = null)
        top -> GuideGrab(vertical = false, index = null)
        left -> GuideGrab(vertical = true, index = null)
        else -> null
    }
}

/** [guides] after [guide] was dropped: in place of the one at [index] (or added), or removed when dropped on its ruler. */
private fun dropGuide(camera: ViewportCamera, guides: List<Guide>, guide: Guide, ruler: Float, index: Int?, origin: Offset): List<Guide> {
    val gone = screenAt(camera, guide) + (if (guide.vertical) origin.x else origin.y) < ruler
    val list = guides.toMutableList()
    if (index != null) list.removeAt(index)
    if (!gone) list.add(index ?: list.size, guide)
    return list
}

/**
 * The pointer side of rulers and guides: dragging out of a ruler places a guide, dragging a guide moves it, and the
 * mouse shows a resize pointer over either. Touch reaches [FingerReach] past a guide or ruler, since a fingertip
 * covers far more than a pointer. Presses here win over the scene's own. This goes on the viewport itself, not on a
 * layer over the scene: an overlay that takes pointer input hides the pointer from the scene below it, so the
 * scene would get neither clicks nor hover. With [band] the rulers sit beside the view rather than over it.
 */
internal fun Modifier.guideGrabbing(
    camera: ViewportCamera,
    guides: List<Guide>,
    onGuidesChange: ((List<Guide>) -> Unit)?,
    drag: GuideDragState,
    band: Boolean,
): Modifier = composed {
    val current by rememberUpdatedState(guides)
    val change by rememberUpdatedState(onGuidesChange)
    val icon = drag.hover?.let { if (it.vertical) resizeSideways else resizeUpDown }
    pointerInput(camera, drag, band) {
        val ruler = RulerSize.toPx()
        val origin = if (band) Offset(ruler, ruler) else Offset.Zero
        val finger = FingerReach.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (change == null) return@awaitEachGesture
            if (down.type == PointerType.Mouse && !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
            val touch = down.type == PointerType.Touch
            val grab = grabAt(camera, current, down.position, ruler, if (touch) finger else GRAB, if (touch) finger else 0f, drag.blocked, origin)
                ?: return@awaitEachGesture
            down.consume()
            drag.moving = grab.index
            drag.pending = grab.index?.let { current[it] }
            dragUntilUp(PointerEventPass.Initial) { p ->
                val scene = camera.toScene(p - origin)
                drag.pending = Guide(grab.vertical, if (grab.vertical) scene.x else scene.y)
            }
            drag.pending?.let { change?.invoke(dropGuide(camera, current, it, ruler, grab.index, origin)) }
            drag.pending = null
            drag.moving = null
        }
    }.pointerInput(camera, drag, band) {
        val ruler = RulerSize.toPx()
        val origin = if (band) Offset(ruler, ruler) else Offset.Zero
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pointer = event.changes.firstOrNull() ?: continue
                when {
                    event.type == PointerEventType.Exit -> drag.hover = null
                    // Kept while pressed, so the pointer stays put through a drag.
                    !pointer.pressed -> drag.hover =
                        if (change == null) null else grabAt(camera, current, pointer.position, ruler, GRAB, 0f, drag.blocked, origin)
                }
            }
        }
    }.then(if (icon != null) Modifier.pointerHoverIcon(icon, overrideDescendants = true) else Modifier)
}

/**
 * Rulers on the viewport's top and left edges, in scene units at [camera]'s zoom, and the [guides] across it, with
 * the guide [drag] in progress. Only draws: [guideGrabbing] on the viewport handles the pointer. [Viewport] draws
 * these when given rulers. With [band] the rulers sit beside the view, which starts past them, and the guides are
 * left to the view's renderer.
 */
@Composable
internal fun RulersAndGuides(camera: ViewportCamera, guides: List<Guide>, drag: GuideDragState, band: Boolean) {
    // The scene's axes run left to right in every language, so the rulers do too.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Rulers(camera, guides, drag, band) }
}

@Composable
private fun Rulers(camera: ViewportCamera, guides: List<Guide>, drag: GuideDragState, band: Boolean) {
    val c = Stretch.colors
    val current by rememberUpdatedState(guides)
    // Over the view, each ruler starts under the corner square; as a band, it starts past it.
    val lead = if (band) RulerSize else 0.dp
    Box(Modifier.fillMaxSize()) {
        if (!band) {
            Canvas(Modifier.fillMaxSize()) {
                for (g in drag.shown(current)) {
                    val at = screenAt(camera, g)
                    if (g.vertical) {
                        drawLine(c.accent, Offset(at, 0f), Offset(at, size.height), 1.dp.toPx())
                    } else {
                        drawLine(c.accent, Offset(0f, at), Offset(size.width, at), 1.dp.toPx())
                    }
                }
            }
        }
        Ruler(camera, vertical = false, underCorner = !band, Modifier.padding(start = lead).height(RulerSize).fillMaxWidth())
        Ruler(camera, vertical = true, underCorner = !band, Modifier.padding(top = lead).width(RulerSize).fillMaxHeight())
        Canvas(Modifier.size(RulerSize)) { drawRect(c.surfacePanel) }
    }
}

/**
 * One ruler, whose own pixels line up with [camera]'s view along it. [underCorner] says its start is under the
 * corner square, where labels are left out.
 */
@Composable
private fun Ruler(camera: ViewportCamera, vertical: Boolean, underCorner: Boolean, modifier: Modifier) {
    val c = Stretch.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = Stretch.type.valueSm.copy(color = c.inkSubtle)
    val description = stringResource(if (vertical) Res.string.ruler_vertical else Res.string.ruler_horizontal)
    Canvas(modifier.clipToBounds().semantics { contentDescription = description }) {
        drawRect(c.surfacePanel)
        val step = rulerStep(camera.zoom)
        val minor = step / if (abs(step / 10f.pow(floor(log10(step))) - 2f) < 0.01f) 4 else 5
        val length = if (vertical) size.height else size.width
        val from = camera.toScene(Offset.Zero).let { if (vertical) it.y else it.x }
        val to = camera.toScene(Offset(length, length)).let { if (vertical) it.y else it.x }
        var v = floor(from / minor) * minor
        val thickness = if (vertical) size.width else size.height
        val hidden = if (underCorner) thickness else 0f
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
                // Labels that would run under the corner square or off the start are left out rather than cut.
                if (vertical && at - label.size.width < hidden || !vertical && at < hidden) {
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
