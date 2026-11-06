package dev.chieppa.stretch.diff

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DiffRulerTest {
    private val file = diffTexts(
        (1..200).joinToString("\n") { "line $it" },
        (1..200).joinToString("\n") { if (it == 20 || it == 190) "changed $it " + "word ".repeat(if (it == 190) 40 else 0) else "line $it" },
        "notes.txt",
    )

    @Test
    fun wrappedRowsTakeTheirShare() {
        val rows = diffRows(listOf(file), split = false)
        val flat = rulerRows(rows, wrap = false, columns = 20)
        assertEquals(rows.size.toFloat(), flat.length)
        val wrapped = rulerRows(rows, wrap = true, columns = 20)
        assertTrue(wrapped.length >= rows.size + 9, "the long added line takes about ten rows")
    }

    @Test
    fun foldedLinesMarkTheirFold() {
        val rows = diffRows(listOf(file), split = true)
        val at = lineRows(rows, listOf(file))
        val fold = at.getValue(Triple(0, DiffSide.Old, 100))
        assertTrue(rows[fold] is DiffRow.Fold)
        assertEquals(at[Triple(0, DiffSide.New, 100)], fold)
        assertTrue(rows[at.getValue(Triple(0, DiffSide.New, 20))] is DiffRow.Split)
    }

    @Test
    fun pressingOnTheRulerScrolls() = runComposeUiTest {
        val unfolded = diffRows(listOf(file), split = false).filterIsInstance<DiffRow.Fold>().map { it.id }.toSet()
        val state = DiffViewerState().apply { unfolded.forEach(::unfold) }
        setStretchContent(StretchThemes.Ink) { DiffViewer(listOf(file), Modifier.size(700.dp, 400.dp), state, showBar = false) }
        onNodeWithText("line 180").assertDoesNotExist()
        onRoot().performTouchInput { click(Offset(width - 5.dp.toPx(), height * 0.92f)) }
        waitForIdle()
        onNodeWithText("line 180").assertExists()
    }
}
