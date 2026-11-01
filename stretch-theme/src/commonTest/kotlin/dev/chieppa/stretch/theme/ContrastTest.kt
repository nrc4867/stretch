package dev.chieppa.stretch.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

/** The contrast pairs the design system's accessibility notes promise, checked for every theme. */
class ContrastTest {
    private fun ratio(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    @Test
    fun everyThemeMeetsItsContrastPairs() {
        val failures = StretchThemes.all.flatMap { theme ->
            val c = theme.colors
            val grounds = listOf(c.surfaceCanvas, c.surfaceRail, c.surfacePanel, c.surfaceFloat, c.surfaceControl)
            val texts = listOf(c.inkStrong, c.ink, c.inkControl, c.inkMuted, c.inkSubtle)
            val text = texts.flatMap { fg -> grounds.map { bg -> Triple(fg, bg, 4.5) } } + listOf(
                Triple(c.onInk, c.onFill, 4.5),
                Triple(c.warnInk, c.warnWash, 4.5),
                Triple(c.inkMuted, c.surfaceControlHi, 4.5),
                Triple(c.inkStrong, c.neutralOn, 4.5),
            )
            val marks = listOf(
                Triple(c.focus, c.surfacePanel, 3.0),
                Triple(c.focus, c.surfaceCanvas, 3.0),
                Triple(c.focus, c.surfaceFloat, 3.0),
                Triple(c.ringSelected, c.surfacePanel, 3.0),
                Triple(c.toggleOffEdge, c.surfacePanel, 3.0),
                Triple(c.toggleOnEdge, c.surfacePanel, 3.0),
                Triple(c.affected, c.surfaceWell, 3.0),
                Triple(c.inkStrong, c.surfaceWell, 3.0),
            )
            val knob = max(ratio(c.knob, c.track), ratio(c.knobEdge, c.track))
            (text + marks).filter { (fg, bg, min) -> ratio(fg, bg) < min }.map { "${theme.id}: $it" } +
                listOfNotNull(if (knob < 3.0) "${theme.id}: slider knob" else null)
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
