package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchShadow
import dev.chieppa.stretch.theme.stretchShadow

/** The look of anything floating over the viewport: float fill, strong border and an optional shadow. */
@Composable
internal fun Modifier.floating(shape: Shape, shadow: StretchShadow?): Modifier {
    val c = Stretch.colors
    val shadowed = if (shadow != null) stretchShadow(shadow, shape) else this
    return shadowed.clip(shape).background(c.surfaceFloat).border(Stretch.stroke, c.lineStrong, shape)
}
