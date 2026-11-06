package dev.chieppa.stretch.code

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.chieppa.stretch.theme.Stretch

/** The colors code views mark their overview rulers with, from the theme. */
@Immutable
class RulerColors(
    val added: Color,
    val removed: Color,
    val warning: Color,
    val error: Color,
    val emphasis: Color,
    val selected: Color,
    val match: Color,
    val currentMatch: Color,
) {
    /** The mark of a line washed in [tone]. */
    fun of(tone: LineTone): Color = when (tone) {
        LineTone.Added -> added
        LineTone.Removed -> removed
        LineTone.Warning -> warning
        LineTone.Error -> error
        LineTone.Emphasis -> emphasis
        LineTone.Selected -> selected
    }
}

/** The current theme's [RulerColors]. */
val rulerColors: RulerColors
    @Composable
    @ReadOnlyComposable
    get() {
        val c = Stretch.colors
        return RulerColors(c.diffAddInk, c.diffDelInk, c.warnInk, c.errorInk, c.inkSubtle, c.inkMuted, c.accent, c.inkStrong)
    }
