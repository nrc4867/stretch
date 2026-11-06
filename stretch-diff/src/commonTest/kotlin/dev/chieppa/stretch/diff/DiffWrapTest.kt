package dev.chieppa.stretch.diff

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DiffWrapTest {
    private val long = "word ".repeat(50).trimEnd()
    private val file = diffTexts("first\n$long\nlast", "first\nshort\nlast", "notes.txt")

    @Test
    fun sideBySideRowsWrapAndStayLevel() = runComposeUiTest {
        setStretchContent(StretchThemes.InkDark) { DiffViewer(listOf(file), Modifier.size(700.dp, 600.dp), rememberDiffViewerState(split = true, wrap = true)) }
        val wrapped = onNodeWithText(long).getUnclippedBoundsInRoot()
        val short = onNodeWithText("short").getUnclippedBoundsInRoot()
        assertTrue(wrapped.height > short.height * 3, "the long line wraps")
        assertEquals(wrapped.top, short.top, "both sides of a row start level")
        val numbers = onAllNodesWithText("2").fetchSemanticsNodes().map { it.boundsInRoot.top }
        assertEquals(2, numbers.size)
        assertTrue(numbers.all { it == numbers.first() }, "both numbers sit on the row's first line")
        onAllNodesWithText("last").fetchSemanticsNodes().forEach { node ->
            assertTrue(node.boundsInRoot.top >= onNodeWithText(long).fetchSemanticsNode().boundsInRoot.bottom, "the next row starts under the taller side")
        }
    }

    @Test
    fun unifiedRowsWrap() = runComposeUiTest {
        setStretchContent(StretchThemes.Ink) { DiffViewer(listOf(file), Modifier.size(500.dp, 600.dp), rememberDiffViewerState(wrap = true)) }
        val wrapped = onNodeWithText(long).getUnclippedBoundsInRoot()
        assertTrue(wrapped.height > onNodeWithText("short").getUnclippedBoundsInRoot().height * 3)
        assertTrue(wrapped.right <= 500.dp)
    }

    @Test
    fun dragSelectsWrappedLines() = runComposeUiTest {
        val added = diffTexts("", (1..60).joinToString("\n") { "row $it " + "word ".repeat(30) }, null, "added.txt")
        val state = DiffViewerState(wrap = true)
        var reported: DiffLineRange? = null
        setStretchContent(StretchThemes.Ink) { DiffViewer(listOf(added), Modifier.size(500.dp, 900.dp), state, slots = DiffSlots(onLineRangeSelect = { reported = it })) }
        val from = onNodeWithText("2").getUnclippedBoundsInRoot()
        val to = onNodeWithText("4").getUnclippedBoundsInRoot()
        onNodeWithText("2").performTouchInput {
            down(center)
            repeat(4) { moveBy(Offset(0f, ((to.top - from.top).value * density) / 4)) }
            up()
        }
        assertEquals(DiffLineRange(0, added, DiffSide.New, 2, 4), reported)
    }
}
