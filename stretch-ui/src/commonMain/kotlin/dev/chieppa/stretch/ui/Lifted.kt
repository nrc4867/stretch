package dev.chieppa.stretch.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.zIndex
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.stretchShadow

/**
 * Draws this element as a thing being moved: above its siblings, on [fill] with a strong edge in [shape] and the pop
 * shadow under it, its content clipped to [shape]. A sortable row or tile and a drag's preview wear it.
 */
@Composable
fun Modifier.lifted(shape: Shape, fill: Color = Stretch.colors.surfaceFloat): Modifier =
    zIndex(1f).stretchShadow(Stretch.shadows.pop, shape).framedSurface(shape, fill, Stretch.colors.lineStrong, Stretch.stroke)
