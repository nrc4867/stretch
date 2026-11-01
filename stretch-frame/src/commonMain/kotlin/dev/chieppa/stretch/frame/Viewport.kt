package dev.chieppa.stretch.frame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

/** How far the viewport's corner UI sits in from its edges. */
private val CornerInset = 12.dp

/** The space between two pieces of corner UI that would otherwise meet. */
private val CornerGap = 8.dp

/** The grid's spacing at zoom 1, and the closest its lines get before every fourth one is kept. */
private val GridStep = 32.dp
private val GridMinStep = 12.dp
private const val GRID_THIN_OUT = 4

/**
 * The slot the app renders into: the well color with a grid. Floating UI goes in its corners, 12dp in:
 * [topStart] for the toolbar, [topEnd] for view tools like the [ViewNavigator], [bottomStart] and [bottomEnd] for
 * readouts, and [bottomCenter] for passing messages such as toasts. When a viewport is too narrow for a pair side by
 * side, the end one moves under (or above) the start one instead of overlapping it, and the centre one rises above
 * the bottom corners it would touch. [content] draws the scene.
 *
 * With a [camera], the wheel, middle button and two-finger touch move the view (see [viewportNavigation]) and the grid
 * follows it. [rulers] adds rulers on the top and left edges in scene units, with [guides] across the view that
 * people drag out of the rulers and back; each change comes back through [onGuidesChange], and [guideDrag] holds the
 * drag in progress.
 *
 * Set [nativeContent] when [content] is a [NativeSurface], which Compose cannot draw over. The rulers then form a band
 * with the content beside them rather than under them, so the camera's origin is the content's top-left corner, which
 * is also the surface's pixel origin. The corners show in popups of their own, which needs `NativeSurfaces.enable()`,
 * and never take focus. Guides, a [marquee] and [transformHandles] keep working, but their drawing is the renderer's:
 * add [guidesOverlay] (with [GuideDragState.shown]), [marqueeOverlay] and [handlesOverlay] to the scene's overlay.
 * Pointer modifiers from other packages that also move views, such as a 3D camera's, go on [modifier]: they see
 * events first, and what they consume the viewport's own navigation leaves alone.
 */
@Composable
fun Viewport(
    modifier: Modifier = Modifier,
    grid: Boolean = true,
    camera: ViewportCamera? = null,
    rulers: Boolean = false,
    guides: List<Guide> = emptyList(),
    onGuidesChange: ((List<Guide>) -> Unit)? = null,
    guideDrag: GuideDragState = rememberGuideDragState(),
    nativeContent: Boolean = false,
    topStart: (@Composable () -> Unit)? = null,
    topEnd: (@Composable () -> Unit)? = null,
    bottomStart: (@Composable () -> Unit)? = null,
    bottomEnd: (@Composable () -> Unit)? = null,
    bottomCenter: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = Stretch.colors
    val withRulers = rulers && camera != null
    val band = nativeContent && withRulers
    val navigation = if (camera != null) Modifier.viewportNavigation(camera) else Modifier
    Box(
        modifier.fillMaxSize().clipToBounds().background(c.surfaceWell)
            .then(if (band) Modifier else navigation)
            .then(if (withRulers) Modifier.guideGrabbing(camera, guides, onGuidesChange, guideDrag, band) else Modifier),
    ) {
        Box(if (band) Modifier.fillMaxSize().absolutePadding(left = RulerSize, top = RulerSize).then(navigation) else Modifier.fillMaxSize()) {
            if (grid && !nativeContent) ViewportGrid(camera)
            content()
        }
        if (withRulers) RulersAndGuides(camera, guides, guideDrag, band)
        val slots = listOf(topStart, topEnd, bottomStart, bottomEnd, bottomCenter)
        val lead = if (withRulers) RulerSize else 0.dp
        if (nativeContent) {
            ViewportCornerPopups(slots, lead) { guideDrag.blocked = it }
        } else {
            ViewportCorners(slots, lead) { guideDrag.blocked = it }
        }
    }
}

/** Grid lines over the viewport, following [camera] when there is one. */
@Composable
private fun ViewportGrid(camera: ViewportCamera?) {
    val line = Stretch.colors.gridLine
    Canvas(Modifier.fillMaxSize()) {
        var step = GridStep.toPx() * (camera?.zoom ?: 1f)
        while (step < GridMinStep.toPx()) step *= GRID_THIN_OUT
        val origin = camera?.offset ?: Offset.Zero
        var x = ((origin.x % step) + step) % step
        while (x < size.width) {
            drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f)
            x += step
        }
        var y = ((origin.y % step) + step) % step
        while (y < size.height) {
            drawLine(line, Offset(0f, y), Offset(size.width, y), 1f)
            y += step
        }
    }
}

/**
 * The corner margins in pixels. [lead] keeps the top and left edges clear for rulers, which sit on the scene's left
 * in every language: the start side left to right, the end side right to left.
 */
internal fun Density.cornerMargins(lead: Dp, layoutDirection: LayoutDirection): CornerMargins {
    val inset = CornerInset.roundToPx()
    val top = inset + lead.roundToPx()
    val ltr = layoutDirection == LayoutDirection.Ltr
    return CornerMargins(top = top, start = if (ltr) top else inset, end = if (ltr) inset else top, bottom = inset, gap = CornerGap.roundToPx())
}

internal fun IntRect.toRect() = Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

/**
 * Places the viewport's corner [slots] (in [CornerSlot] order) in the Compose layout, by [placeCorners]. [onPlaced]
 * gets the area each one covers, in the viewport's pixels, every time they are placed.
 */
@Composable
private fun ViewportCorners(slots: List<(@Composable () -> Unit)?>, lead: Dp, onPlaced: (List<Rect>) -> Unit) {
    val none: @Composable () -> Unit = {}
    Layout(contents = slots.map { it ?: none }, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val margins = cornerMargins(lead, layoutDirection)
        val room = cornerRoom(constraints.maxWidth, constraints.maxHeight, margins)
        val placeables = measurables.map { it.firstOrNull()?.measure(Constraints(maxWidth = room.width, maxHeight = room.height)) }
        val sizes = placeables.map { p -> p?.let { IntSize(it.width, it.height) } }
        val places = placeCorners(sizes, constraints.maxWidth, constraints.maxHeight, margins, layoutDirection == LayoutDirection.Ltr)
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { i, p -> places[i]?.let { p?.place(it.left, it.top) } }
            onPlaced(places.mapNotNull { it?.toRect() })
        }
    }
}
