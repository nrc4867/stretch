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
                Triple(c.onAccent, c.accent, 4.5),
                Triple(c.warnInk, c.warnWash, 4.5),
                Triple(c.inkMuted, c.surfaceControlHi, 4.5),
                Triple(c.inkStrong, c.neutralOn, 4.5),
            )
            val syntax = listOf(c.ink, c.syntaxKeyword, c.syntaxString, c.syntaxNumber, c.syntaxComment, c.syntaxType, c.syntaxFunction)
            val code = syntax.flatMap { fg ->
                listOf(c.surfacePanel, c.surfaceFloat, c.diffAddWash, c.diffDelWash).map { bg -> Triple(fg, bg, 4.5) }
            } + listOf(Triple(c.ink, c.diffAddStrong, 4.5), Triple(c.ink, c.diffDelStrong, 4.5))
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
            (text + code + marks + selected(c) + addedAndRemoved(c)).filter { (fg, bg, min) -> ratio(fg, bg) < min }.map { "${theme.id}: $it" } +
                listOfNotNull(if (knob < 3.0) "${theme.id}: slider knob" else null)
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    /**
     * A selected row keeps the app's own text in it, such as counts, status letters and muted names, so every text
     * token reads on the on fill. While it is pressed its label still reads.
     */
    private fun selected(c: StretchColors): List<Triple<Color, Color, Double>> {
        val inks = listOf(
            c.inkStrong, c.ink, c.inkControl, c.inkMuted, c.inkSubtle, c.onInk, c.deltaUp, c.deltaDown,
            c.diffAddInk, c.diffDelInk, c.warnInk, c.errorInk,
            c.syntaxKeyword, c.syntaxString, c.syntaxNumber, c.syntaxComment, c.syntaxType, c.syntaxFunction,
        )
        return inks.map { Triple(it, c.onFill, 4.5) } + listOf(c.onInk, c.ink, c.inkStrong).map { Triple(it, c.onFillPress, 4.5) }
    }

    /** Added and removed counts, deltas and diff markers read wherever they show: surfaces, diff washes, selected rows. */
    private fun addedAndRemoved(c: StretchColors): List<Triple<Color, Color, Double>> {
        val inks = listOf(c.diffAddInk, c.diffDelInk, c.deltaUp, c.deltaDown)
        val grounds = listOf(
            c.surfaceCanvas, c.surfaceRail, c.surfacePanel, c.surfaceFloat, c.surfaceControl, c.surfaceControlHi,
            c.diffAddWash, c.diffDelWash, c.onFill, c.neutralOn,
        )
        return inks.flatMap { fg -> grounds.map { bg -> Triple(fg, bg, 4.5) } }
    }
}
