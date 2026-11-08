package dev.chieppa.stretch.code

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import dev.chieppa.stretch.theme.Stretch

/** The colors [CodeDecoration]s and the symbol under a held Ctrl or Cmd draw with, from the theme. */
@Immutable
class CodeDecorationLook(
    val reference: Color,
    val definition: Color,
    val accent: Color,
    val warning: Color,
    val error: Color,
    /** The text and underline of the symbol a Ctrl or Cmd click would open. */
    val link: Color,
) {
    fun of(style: CodeDecorationStyle): Color = when (style) {
        CodeDecorationStyle.Reference -> reference
        CodeDecorationStyle.Definition -> definition
        CodeDecorationStyle.Accent -> accent
        CodeDecorationStyle.Warning -> warning
        CodeDecorationStyle.Error -> error
    }
}

/** How much of the theme's color for things affected a [CodeDecorationStyle.Reference] wash shows. */
private const val REFERENCE_ALPHA = 0.16f

/** How much a [CodeDecorationStyle.Definition] wash shows, enough to read over a reference's. */
private const val DEFINITION_ALPHA = 0.32f

/** The theme's [CodeDecorationLook]. */
@Composable
fun rememberCodeDecorationLook(): CodeDecorationLook {
    val c = Stretch.colors
    return remember(c) {
        CodeDecorationLook(
            reference = c.affected.copy(alpha = REFERENCE_ALPHA),
            definition = c.affected.copy(alpha = DEFINITION_ALPHA),
            accent = c.ringSelected,
            warning = c.warnEdge,
            error = c.errorEdge,
            link = c.affected,
        )
    }
}
