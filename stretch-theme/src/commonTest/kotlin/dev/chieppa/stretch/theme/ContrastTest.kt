package dev.chieppa.stretch.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertTrue

/** The contrast pairs the design system's accessibility notes promise, checked for every theme. */
class ContrastTest {
    private fun ratio(a: Color, b: Color): Double = contrastRatio(a, b)

    @Test
    fun everyThemeMeetsItsContrastPairs() {
        val failures = StretchThemes.all.flatMap { theme ->
            val c = theme.colors
            val grounds = listOf(c.surfaceCanvas, c.surfaceRail, c.surfacePanel, c.surfaceFloat, c.surfaceControl, c.surfaceControlHi)
            val texts = listOf(c.inkStrong, c.ink, c.inkControl, c.inkMuted, c.inkSubtle)
            val text = texts.flatMap { fg -> grounds.map { bg -> Triple(fg, bg, 4.5) } } + listOf(
                Triple(c.onInk, c.onFill, 4.5),
                Triple(c.onAccent, c.accent, 4.5),
                Triple(c.warnInk, c.warnWash, 4.5),
                Triple(c.inkStrong, c.neutralOn, 4.5),
            )
            val syntax = listOf(c.ink, c.syntaxKeyword, c.syntaxString, c.syntaxNumber, c.syntaxComment, c.syntaxType, c.syntaxFunction)
            // Code is drawn on panels, emphasised and selected lines, line washes and the changed words within a line.
            val codeGrounds = listOf(
                c.surfacePanel, c.surfaceFloat, c.surfaceControl, c.surfaceControlHi,
                c.diffAddWash, c.diffDelWash, c.diffAddStrong, c.diffDelStrong,
            )
            val code = syntax.flatMap { fg -> codeGrounds.map { bg -> Triple(fg, bg, 4.5) } }
            val accentText = (grounds + c.onFill).map { Triple(c.accentInk, it, 4.5) }
            // The symbol a Ctrl click would open is drawn in the affected color over the code's grounds.
            val codeLink = (codeGrounds - setOf(c.diffAddStrong, c.diffDelStrong)).map { Triple(c.affected, it, 4.5) }
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
            (text + code + accentText + codeLink + marks + selected(c) + addedAndRemoved(c) + lightReading(theme)).filter { (fg, bg, min) -> ratio(fg, bg) < min }.map { "${theme.id}: $it" } +
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

    /**
     * Dark text on a light ground reads thinner than light text on a dark one, so a light theme keeps what is read at
     * length, secondary text and code, at 7:1 on the grounds it is read on, hints at 5.5:1 and selected text at 7:1.
     */
    private fun lightReading(theme: StretchTheme): List<Triple<Color, Color, Double>> {
        if (theme.isDark) return emptyList()
        val c = theme.colors
        val reading = listOf(c.inkMuted, c.syntaxKeyword, c.syntaxString, c.syntaxNumber, c.syntaxComment, c.syntaxType, c.syntaxFunction)
        val grounds = listOf(c.surfacePanel, c.surfaceFloat)
        return reading.flatMap { fg -> grounds.map { bg -> Triple(fg, bg, 7.0) } } +
            grounds.map { Triple(c.inkSubtle, it, 5.5) } +
            listOf(Triple(c.onInk, c.onFill, 7.0), Triple(c.accentInk, c.surfacePanel, 7.0))
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
