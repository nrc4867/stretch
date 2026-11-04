package dev.chieppa.stretch.ui

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp

/**
 * A [fill] with an [edge] border of [stroke] in [shape], for a surface with a shadow behind it, and its content clipped
 * to [shape]. The fill stops under the middle of the border, so it never blends into the soft pixels of a curved edge,
 * where it would show as a light line against a dark shadow.
 */
fun Modifier.framedSurface(shape: Shape, fill: Color, edge: Color, stroke: Dp): Modifier = drawBehind {
    val inset = stroke.toPx() / 2
    val outline = shape.createOutline(Size(size.width - inset * 2, size.height - inset * 2), layoutDirection, this)
    translate(inset, inset) { drawOutline(outline, fill) }
}.border(stroke, edge, shape).clip(shape)
