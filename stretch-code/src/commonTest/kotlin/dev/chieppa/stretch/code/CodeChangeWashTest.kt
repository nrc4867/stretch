package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class CodeChangeWashTest {
    private val code = CodeText.of("added line with some words after it\nplain")
    private val tones = { line: Int -> if (line == 1) LineTone.Added else null }

    /** The color [fromStart] pixels into the row holding [text], and just past the row's text. */
    private fun ComposeUiTest.colorsOf(text: String, fromStart: Int): Pair<Color, Color> {
        val bounds = onNodeWithText(text).getUnclippedBoundsInRoot()
        val y = (((bounds.top + bounds.bottom) / 2).value * density.density).toInt()
        val pixels = onRoot().captureToImage().toPixelMap()
        return pixels[fromStart, y] to pixels[((bounds.right.value + 4) * density.density).toInt(), y]
    }

    @Test
    fun aChangedLineIsWashedWholeByDefault() = runComposeUiTest {
        var look: CodeLook? = null
        setStretchContent(StretchThemes.Ink) {
            look = rememberCodeLook()
            CodeViewer(code, Modifier.size(500.dp, 200.dp), tones = tones, changeBars = false)
        }
        val (gutter, code) = colorsOf("added line with some words after it", fromStart = 6)
        assertEquals(look!!.wash(LineTone.Added), gutter)
        assertEquals(look!!.wash(LineTone.Added), code)
    }

    @Test
    fun aGutterWashLeavesTheCodePlain() = runComposeUiTest {
        var look: CodeLook? = null
        setStretchContent(StretchThemes.Ink) {
            look = rememberCodeLook(ChangeWash.Gutter)
            CodeViewer(code, Modifier.size(500.dp, 200.dp), tones = tones, changeBars = false, changeWash = ChangeWash.Gutter)
        }
        val (gutter, code) = colorsOf("added line with some words after it", fromStart = 6)
        assertEquals(look!!.wash(LineTone.Added), gutter)
        assertNotEquals(look!!.wash(LineTone.Added), code)
    }

}
