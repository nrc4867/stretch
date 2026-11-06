package dev.chieppa.stretch.diff

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.code.LineTone
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class DiffSelectionTest {
    /** A new file of 200 lines, so every number shows once, on the new side. */
    private val added = diffTexts("", (1..200).joinToString("\n") { "row $it" }, null, "added.txt")
    private val changed = diffTexts(
        (1..40).joinToString("\n") { "line $it" },
        (1..40).joinToString("\n") { if (it == 20) "changed line" else "line $it" },
        "notes.txt",
    )

    @Test
    fun findsLinesInRows() {
        val unified = diffRows(listOf(changed), split = false, unfolded = setOf(FoldId(0, 0, 3), FoldId(0, 0, 23)))
        val at = rowOfLine(unified, 0, DiffSide.New, 20)!!
        assertEquals(20, unified[at].numberOn(DiffSide.New))
        assertNull(unified[at].numberOn(DiffSide.Old))
        assertEquals(0, unified[at].fileIndex)
        val split = diffRows(listOf(changed), split = true)
        val folded = rowOfLine(split, 0, DiffSide.Old, 5)!!
        // Line 5 is folded away, so the nearest line after it that shows stands in for it.
        assertEquals(true, split[folded].numberOn(DiffSide.Old)!! > 5)
        assertNull(rowOfLine(split, 3, DiffSide.Old, 1))
    }

    @Test
    fun dragOverNumbersSelectsARange() = runComposeUiTest {
        val state = DiffViewerState()
        var reported: DiffLineRange? = null
        var clicked: DiffLineRef? = null
        val slots = DiffSlots(onLineClick = { clicked = it }, onLineRangeSelect = { reported = it })
        setContent { StretchTheme(StretchThemes.Ink) { DiffViewer(listOf(added), Modifier.size(700.dp, 600.dp), state, slots = slots) } }
        val from = onNodeWithText("3").getUnclippedBoundsInRoot()
        val to = onNodeWithText("7").getUnclippedBoundsInRoot()
        onNodeWithText("3").performTouchInput {
            down(center)
            repeat(4) { moveBy(androidx.compose.ui.geometry.Offset(0f, ((to.top - from.top).value * density) / 4)) }
            up()
        }
        assertEquals(DiffLineRange(0, added, DiffSide.New, 3, 7), reported)
        assertEquals(reported, state.lineSelection)
        assertNull(clicked)
        onNodeWithText("9").performClick()
        assertEquals(9, clicked?.number)
        assertEquals(DiffSide.New, clicked?.side)
        assertEquals(DiffLineRange(0, added, DiffSide.New, 9, 9), reported)
    }

    @Test
    fun drawsGutterMarksAndTones() = runComposeUiTest {
        val slots = DiffSlots(
            gutterMark = { ref -> if (ref.number == 4 && ref.side == DiffSide.New) BasicText("◆") },
            lineTone = { ref -> if (ref.number == 5) LineTone.Emphasis else null },
        )
        setContent { StretchTheme(StretchThemes.WorkshopDark) { DiffViewer(listOf(added), Modifier.size(700.dp, 600.dp), slots = slots) } }
        onAllNodesWithText("◆").assertCountEquals(1)
        onNodeWithText("row 5").assertExists()
    }

    @Test
    fun scrollsToALineAndReportsTheTopOne() = runComposeUiTest {
        val state = DiffViewerState()
        setContent { StretchTheme(StretchThemes.Ink) { DiffViewer(listOf(added), Modifier.size(700.dp, 400.dp), state) } }
        onNodeWithText("row 150").assertDoesNotExist()
        runOnIdle { runBlocking { state.scrollToLine(0, DiffSide.New, 150) } }
        waitForIdle()
        onNodeWithText("row 150").assertExists()
        assertEquals(0 to 150, state.firstVisibleLine(DiffSide.New))
    }
}
