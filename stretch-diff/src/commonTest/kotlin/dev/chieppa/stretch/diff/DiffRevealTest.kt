package dev.chieppa.stretch.diff

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.code.CodeFlash
import dev.chieppa.stretch.code.CodeRange
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchThemes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DiffRevealTest {
    private val file = diffTexts(
        (1..200).joinToString("\n") { "line $it" },
        (1..200).joinToString("\n") { if (it == 100) "changed line" else "line $it" },
        "notes.txt",
    )

    @Test
    fun revealingALineUnderAFoldOpensTheFoldAndScrollsToIt() = runComposeUiTest {
        val state = DiffViewerState()
        lateinit var scope: CoroutineScope
        setStretchContent(StretchThemes.InkDark) {
            scope = rememberCoroutineScope()
            DiffViewer(listOf(file), Modifier.size(800.dp, 400.dp), state)
        }
        onAllNodesWithText("line 40", substring = false).assertCountEquals(0)
        scope.launch { state.revealLine(0, DiffSide.New, 40) }
        waitForIdle()
        onNodeWithText("line 40").assertExists()
        assertEquals(1, state.unfolded.size)
        assertTrue(state.firstVisibleLine(DiffSide.New)!!.second in 37..40)
    }

    @Test
    fun aFlashFadesAndIsForgotten() = runComposeUiTest {
        val state = DiffViewerState()
        setStretchContent(StretchThemes.InkDark) { DiffViewer(listOf(file), Modifier.size(800.dp, 400.dp), state) }
        mainClock.autoAdvance = false
        state.flash(0, DiffSide.New, CodeRange(100, 0, 7))
        mainClock.advanceTimeByFrame()
        assertEquals(DiffMatch(0, DiffSide.New, 100, 0, 7), state.flashing)
        mainClock.advanceTimeBy((CodeFlash.HOLD_MS + CodeFlash.FADE_MS + FRAME_SLACK_MS).toLong())
        assertNull(state.flashing)
    }

    @Test
    fun scrollingPastTheEndLeavesRoomAfterTheLastRow() = runComposeUiTest {
        val state = DiffViewerState()
        lateinit var scope: CoroutineScope
        setStretchContent(StretchThemes.InkDark) {
            scope = rememberCoroutineScope()
            DiffViewer(listOf(file), Modifier.size(800.dp, 400.dp), state, scrollPastEnd = 0.5f)
        }
        scope.launch { state.list.scrollToItem(state.list.layoutInfo.totalItemsCount - 1) }
        waitForIdle()
        val info = state.list.layoutInfo
        assertTrue(info.afterContentPadding > 0)
        assertEquals((info.viewportEndOffset - info.viewportStartOffset) / 2, info.afterContentPadding, absoluteTolerance = 1)
    }

    private fun assertEquals(expected: Int, actual: Int, absoluteTolerance: Int) =
        assertTrue(kotlin.math.abs(expected - actual) <= absoluteTolerance, "expected $expected but was $actual")

    private companion object {
        /** A frame or two more than the fade, for the animation's last frame to land. */
        const val FRAME_SLACK_MS = 100
    }
}
