package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchShadow
import dev.chieppa.stretch.theme.stretchShadow
import dev.chieppa.stretch.ui.framedSurface

/** The look of anything floating over the viewport: a [fill], strong border and an optional shadow. */
@Composable
internal fun Modifier.floating(shape: Shape, shadow: StretchShadow?, fill: Color = Stretch.colors.surfaceFloat): Modifier {
    val shadowed = if (shadow != null) stretchShadow(shadow, shape) else this
    return shadowed.framedSurface(shape, fill, Stretch.colors.lineStrong, Stretch.stroke)
}
