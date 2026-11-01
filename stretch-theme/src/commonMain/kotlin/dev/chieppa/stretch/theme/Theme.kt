package dev.chieppa.stretch.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner radii. They come from the theme because ink and workshop themes round differently. */
@Immutable
data class StretchShapes(val seg: Dp, val ctl: Dp, val row: Dp, val card: Dp) {
    val segShape get() = RoundedCornerShape(seg)
    val ctlShape get() = RoundedCornerShape(ctl)
    val rowShape get() = RoundedCornerShape(row)
    val cardShape get() = RoundedCornerShape(card)
    val pillShape get() = RoundedCornerShape(50)
}

/**
 * A full theme: every color token, both shadows and the personality values (border width, radii, display face).
 * Apps pick one of [StretchThemes] or define their own with the same parts.
 */
@Immutable
data class StretchTheme(
    val id: String,
    val name: String,
    val colors: StretchColors,
    val shadows: StretchShadows,
    val shapes: StretchShapes,
    val stroke: Dp,
    val display: DisplayFace,
    val isDark: Boolean,
) {
    val focusStroke: Dp get() = InkStroke.focus
}

object StretchThemes {
    private val inkShapes = StretchShapes(InkRadius.seg, InkRadius.ctl, InkRadius.row, InkRadius.card)
    private val workshopShapes = StretchShapes(seg = 5.dp, ctl = 6.dp, row = 7.dp, card = 10.dp)

    val Ink = StretchTheme(
        "ink", "Ink and paper", InkColors, InkShadows, inkShapes, InkStroke.control, DisplayFace.Rounded, false,
    )
    val InkDark = StretchTheme(
        "ink-dark", "Ink and paper, dark", InkDarkColors, InkDarkShadows, inkShapes, InkStroke.control,
        DisplayFace.Rounded, true,
    )
    val WorkshopDark = StretchTheme(
        "workshop-dark", "Workshop, dark", WorkshopDarkColors, WorkshopDarkShadows, workshopShapes, 1.dp,
        DisplayFace.Sans, true,
    )
    val WorkshopLight = StretchTheme(
        "workshop-light", "Workshop, light", WorkshopLightColors, WorkshopLightShadows, workshopShapes, 1.dp,
        DisplayFace.Sans, false,
    )

    val all = listOf(Ink, InkDark, WorkshopDark, WorkshopLight)
}

val LocalStretchTheme = staticCompositionLocalOf { StretchThemes.Ink }
val LocalStretchType = staticCompositionLocalOf<StretchType> { error("No StretchTheme above this composable") }

@Composable
fun StretchTheme(theme: StretchTheme, content: @Composable () -> Unit) {
    val fonts = stretchFonts()
    val type = remember(fonts, theme.display) { StretchType(fonts, theme.display) }
    CompositionLocalProvider(LocalStretchTheme provides theme, LocalStretchType provides type, content = content)
}

/** The current theme's parts, read inside a [StretchTheme]. */
object Stretch {
    val theme: StretchTheme
        @Composable @ReadOnlyComposable get() = LocalStretchTheme.current
    val colors: StretchColors
        @Composable @ReadOnlyComposable get() = LocalStretchTheme.current.colors
    val shapes: StretchShapes
        @Composable @ReadOnlyComposable get() = LocalStretchTheme.current.shapes
    val shadows: StretchShadows
        @Composable @ReadOnlyComposable get() = LocalStretchTheme.current.shadows
    val type: StretchType
        @Composable @ReadOnlyComposable get() = LocalStretchType.current
    val stroke: Dp
        @Composable @ReadOnlyComposable get() = LocalStretchTheme.current.stroke
}
