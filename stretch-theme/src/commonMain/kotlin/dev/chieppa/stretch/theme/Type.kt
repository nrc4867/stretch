package dev.chieppa.stretch.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.chieppa.stretch.theme.generated.Res
import dev.chieppa.stretch.theme.generated.fredoka
import dev.chieppa.stretch.theme.generated.ibm_plex_mono_medium
import dev.chieppa.stretch.theme.generated.ibm_plex_mono_regular
import dev.chieppa.stretch.theme.generated.ibm_plex_sans
import org.jetbrains.compose.resources.Font

internal class TypeToken(
    val family: String,
    val size: Float,
    val lineHeight: Float,
    val weight: Int,
    val tracking: Float,
)

/** The face used for the app name, drawer and dialog titles and board headlines. */
enum class DisplayFace { Rounded, Sans }

@Immutable
data class StretchFonts(val sans: FontFamily, val mono: FontFamily, val rounded: FontFamily)

/** IBM Plex Sans, IBM Plex Mono and Fredoka, bundled as font resources (SIL Open Font License). */
@Composable
fun stretchFonts(): StretchFonts = StretchFonts(
    sans = FontFamily(
        Font(Res.font.ibm_plex_sans, FontWeight.Normal),
        Font(Res.font.ibm_plex_sans, FontWeight.Medium),
        Font(Res.font.ibm_plex_sans, FontWeight.SemiBold),
    ),
    mono = FontFamily(
        Font(Res.font.ibm_plex_mono_regular, FontWeight.Normal),
        Font(Res.font.ibm_plex_mono_medium, FontWeight.Medium),
    ),
    rounded = FontFamily(
        Font(Res.font.fredoka, FontWeight.Medium),
        Font(Res.font.fredoka, FontWeight.SemiBold),
    ),
)

@Immutable
class StretchType(fonts: StretchFonts, display: DisplayFace) {
    private val displayFamily = if (display == DisplayFace.Rounded) fonts.rounded else fonts.sans

    private val familyOf = mapOf("sans" to fonts.sans, "mono" to fonts.mono, "display" to displayFamily)

    private fun style(token: TypeToken) = TextStyle(
        fontFamily = familyOf.getValue(token.family),
        fontSize = token.size.sp,
        lineHeight = token.lineHeight.sp,
        fontWeight = FontWeight(token.weight),
        letterSpacing = token.tracking.em,
    )

    val body = style(TypeTokens.body)
    val bodySm = style(TypeTokens.bodySm)
    val control = style(TypeTokens.control)
    val chip = style(TypeTokens.chip)
    val eyebrow = style(TypeTokens.eyebrow)
    val drawerTitle = style(TypeTokens.drawerTitle)
    val heading = style(TypeTokens.heading)
    val boardTitle = style(TypeTokens.boardTitle)
    val pageTitle = style(TypeTokens.pageTitle)
    val value = style(TypeTokens.value)
    val valueSm = style(TypeTokens.valueSm)
}
