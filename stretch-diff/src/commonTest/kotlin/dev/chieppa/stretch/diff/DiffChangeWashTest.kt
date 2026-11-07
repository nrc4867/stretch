package dev.chieppa.stretch.diff

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
import dev.chieppa.stretch.code.ChangeWash
import dev.chieppa.stretch.code.CodeLook
import dev.chieppa.stretch.code.LineTone
import dev.chieppa.stretch.code.rememberCodeLook
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class DiffChangeWashTest {
    private val diff = diffTexts("same\n", "same\nan added line\n", "a.txt", "a.txt")

    /** The color near the start of the row holding [text], and just past its text. */
    private fun ComposeUiTest.colorsOf(text: String): Pair<Color, Color> {
        val bounds = onNodeWithText(text, substring = true).getUnclippedBoundsInRoot()
        val y = (((bounds.top + bounds.bottom) / 2).value * density.density).toInt()
        val pixels = onRoot().captureToImage().toPixelMap()
        return pixels[(4 * density.density).toInt(), y] to pixels[((bounds.right.value + 4) * density.density).toInt(), y]
    }

    private fun ComposeUiTest.show(wash: ChangeWash): CodeLook {
        var look: CodeLook? = null
        setStretchContent(StretchThemes.Ink) {
            look = rememberCodeLook(wash)
            DiffViewer(listOf(diff), Modifier.size(700.dp, 300.dp), rememberDiffViewerState(split = false), showBar = false, changeWash = wash)
        }
        return look!!
    }

    @Test
    fun anAddedLineIsWashedWholeByDefault() = runComposeUiTest {
        val look = show(ChangeWash.Line)
        val (gutter, code) = colorsOf("an added line")
        assertEquals(look.wash(LineTone.Added), gutter)
        assertEquals(look.wash(LineTone.Added), code)
    }

    @Test
    fun aGutterWashLeavesTheCodePlain() = runComposeUiTest {
        val look = show(ChangeWash.Gutter)
        val (gutter, code) = colorsOf("an added line")
        assertEquals(look.wash(LineTone.Added), gutter)
        assertNotEquals(look.wash(LineTone.Added), code)
    }
}
