package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.inEveryTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class CodeChangeBarTest {
    private val code = CodeText.of("added\nplain\nremoved")
    private val tones = { line: Int -> if (line == 1) LineTone.Added else if (line == 3) LineTone.Removed else null }

    /** The color at the very start of the row holding [text]. */
    private fun androidx.compose.ui.test.ComposeUiTest.startOf(text: String): Color {
        val bounds = onNodeWithText(text).getUnclippedBoundsInRoot()
        val y = ((bounds.top + bounds.bottom) / 2).value * density.density
        return onRoot().captureToImage().toPixelMap()[1, y.toInt()]
    }

    @Test
    fun barsChangedLinesInTheStrongDiffColors() = runComposeUiTest {
        var colors: RulerColors? = null
        inEveryTheme({
            colors = rulerColors
            CodeViewer(code, Modifier.size(400.dp, 200.dp), tones = tones)
        }) {
            assertEquals(colors!!.added, startOf("added"))
            assertEquals(colors!!.removed, startOf("removed"))
            assertNotEquals(colors!!.added, startOf("plain"))
        }
    }

    @Test
    fun barsCanBeTurnedOff() = runComposeUiTest {
        var colors: RulerColors? = null
        inEveryTheme({
            colors = rulerColors
            CodeViewer(code, Modifier.size(400.dp, 200.dp), tones = tones, changeBars = false)
        }) {
            assertNotEquals(colors!!.added, startOf("added"))
        }
    }
}
