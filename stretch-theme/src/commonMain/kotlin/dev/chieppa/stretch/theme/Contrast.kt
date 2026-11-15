package dev.chieppa.stretch.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/** The WCAG contrast ratio of [a] and [b], from 1 (the same) to 21 (black on white), whichever is lighter. */
fun contrastRatio(a: Color, b: Color): Double {
    val la = a.luminance().toDouble()
    val lb = b.luminance().toDouble()
    return (max(la, lb) + RELATIVE_FLARE) / (min(la, lb) + RELATIVE_FLARE)
}

/** The flare WCAG adds to both luminances, so pure black has a ratio to white of 21. */
private const val RELATIVE_FLARE = 0.05
