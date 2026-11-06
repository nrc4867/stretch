package dev.chieppa.stretch.diff

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.code.CodeLook
import dev.chieppa.stretch.code.rememberCodeLook
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchThemes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DiffMatchTest {
    private val file = diffTexts(
        (1..200).joinToString("\n") { "line $it" },
        (1..200).joinToString("\n") { if (it == 20) "changed line" else "line $it" },
        "notes.txt",
    )

    @Test
    fun findsInEachLineOnTheSideItShowsOn() {
        val small = diffTexts("keep a\ngone a", "keep a\nnew A", "x.txt")
        assertEquals(
            listOf(DiffMatch(0, DiffSide.New, 1, 5, 6), DiffMatch(0, DiffSide.Old, 2, 5, 6), DiffMatch(0, DiffSide.New, 2, 4, 5)),
            findInDiff(listOf(small), "a"),
        )
        assertEquals(listOf(DiffMatch(0, DiffSide.New, 2, 4, 5)), findInDiff(listOf(small), "A", matchCase = true))
        assertEquals(emptyList(), findInDiff(listOf(small), ""))
    }

    @Test
    fun drawsMatchesWithTheCurrentOneApart() = runComposeUiTest {
        val current = DiffMatch(0, DiffSide.New, 20, 0, 7)
        val other = DiffMatch(0, DiffSide.Old, 20, 0, 4)
        var look: CodeLook? = null
        setStretchContent(StretchThemes.InkDark) {
            look = rememberCodeLook()
            DiffViewer(listOf(file), Modifier.size(800.dp, 600.dp), rememberDiffViewerState(split = true), matches = listOf(current, other), currentMatch = current)
        }
        assertTrue(look!!.currentMatch in onNodeWithText("changed line").spanBackgrounds())
        assertTrue(look!!.match in onNodeWithText("line 20").spanBackgrounds())
        assertTrue(look!!.currentMatch !in onNodeWithText("line 20").spanBackgrounds())
    }

    @Test
    fun scrollingToAMatchOpensItsFoldAndFile() = runComposeUiTest {
        val state = DiffViewerState()
        val match = DiffMatch(0, DiffSide.Old, 150, 5, 8)
        var scope: CoroutineScope? = null
        setStretchContent(StretchThemes.Ink) {
            scope = rememberCoroutineScope()
            DiffViewer(listOf(file), Modifier.size(800.dp, 400.dp), state, matches = listOf(match), currentMatch = match)
        }
        onNodeWithText("line 150").assertDoesNotExist()
        runOnIdle { state.toggleFile(0) }
        waitForIdle()
        onAllNodesWithText("changed line").assertCountEquals(0)
        runOnIdle { scope!!.launch { state.scrollToMatch(match) } }
        waitForIdle()
        onNodeWithText("line 150").assertExists()
        assertEquals(emptySet(), state.collapsedFiles)
        val shown = state.list.layoutInfo.visibleItemsInfo.map { it.index }
        assertTrue(state.rows.indexOfFirst { it.numberOn(DiffSide.Old) == 150 } in shown)
    }
}
