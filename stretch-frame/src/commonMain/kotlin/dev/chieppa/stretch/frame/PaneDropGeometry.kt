package dev.chieppa.stretch.frame

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** How far in from a pane's edge, as a share of its size, a dropped tab splits the pane rather than joining it. */
internal const val PANE_EDGE_ZONE = 0.25f

/**
 * The edge of a pane's [body] a tab dropped at [point] splits off, or null in the middle, where it joins the pane.
 * The nearest edge wins within [PANE_EDGE_ZONE] of it. In a right-to-left layout the left edge is the end.
 */
internal fun edgeAt(body: Rect, point: Offset, rtl: Boolean): PaneEdge? {
    if (body.width <= 0f || body.height <= 0f) return null
    val fx = (point.x - body.left) / body.width
    val fy = (point.y - body.top) / body.height
    val left = if (rtl) PaneEdge.End else PaneEdge.Start
    val right = if (rtl) PaneEdge.Start else PaneEdge.End
    val (edge, distance) = listOf(left to fx, right to 1 - fx, PaneEdge.Top to fy, PaneEdge.Bottom to 1 - fy).minBy { it.second }
    return edge.takeIf { distance <= PANE_EDGE_ZONE }
}

/** The part of a pane's [body] a tab dropped at [edge] would take: its half on that side, or all of it for null. */
internal fun dropPreview(body: Rect, edge: PaneEdge?, rtl: Boolean): Rect {
    val toLeft = (edge == PaneEdge.Start) != rtl
    return when (edge) {
        null -> body
        PaneEdge.Top -> body.copy(bottom = body.top + body.height / 2)
        PaneEdge.Bottom -> body.copy(top = body.top + body.height / 2)
        PaneEdge.Start, PaneEdge.End -> if (toLeft) body.copy(right = body.left + body.width / 2) else body.copy(left = body.left + body.width / 2)
    }
}

/**
 * Where a tab dropped at [x] goes in a strip whose other tabs span [spans] (left and right, in strip order): before
 * the first whose middle is past [x], counting from the end in a right-to-left layout.
 */
internal fun stripIndex(spans: List<ClosedFloatingPointRange<Float>>, x: Float, rtl: Boolean): Int =
    spans.count { span -> val middle = (span.start + span.endInclusive) / 2; if (rtl) middle > x else middle < x }
