package dev.chieppa.stretch.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset

/** A shadow token. Ink themes use a hard offset (no blur); workshop themes a soft one. */
@Immutable
data class StretchShadow(val x: Dp, val y: Dp, val blur: Dp, val color: Color)

/** Draws [shadow] behind the content, in [shape]. Only floating things carry a shadow. */
fun Modifier.stretchShadow(shadow: StretchShadow, shape: Shape): Modifier = dropShadow(
    shape = shape,
    shadow = Shadow(radius = shadow.blur, color = shadow.color, offset = DpOffset(shadow.x, shadow.y)),
)
