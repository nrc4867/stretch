package dev.chieppa.stretch.diff

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DiffViewerTest {
    private val file = diffTexts(
        (1..40).joinToString("\n") { "line $it" },
        (1..40).joinToString("\n") { if (it == 20) "changed line" else "line $it" },
        "notes.txt",
    )

    @Test
    fun showsChangesAndFoldsTheRest() = runComposeUiTest {
        setContent { StretchTheme(StretchThemes.Ink) { DiffViewer(listOf(file), Modifier.fillMaxSize()) } }
        onNodeWithText("notes.txt").assertExists()
        onNodeWithText("changed line").assertExists()
        onAllNodesWithText("line 2", substring = false).assertCountEquals(0)
    }

    @Test
    fun aFoldOpensWhenClicked() = runComposeUiTest {
        setContent { StretchTheme(StretchThemes.Ink) { DiffViewer(listOf(file), Modifier.fillMaxSize()) } }
        onNodeWithText("16 unchanged lines").performClick()
        waitForIdle()
        onNodeWithText("line 2").assertExists()
    }

    @Test
    fun switchesToSideBySide() = runComposeUiTest {
        setContent { StretchTheme(StretchThemes.WorkshopDark) { DiffViewer(listOf(file), Modifier.fillMaxSize()) } }
        onNodeWithText("Split").performClick()
        waitForIdle()
        onNodeWithText("changed line").assertExists()
        onNodeWithText("line 20").assertExists()
        // Unchanged lines appear on both sides.
        onAllNodesWithText("line 19").assertCountEquals(2)
    }

    @Test
    fun collapsingAFileHidesItsLines() = runComposeUiTest {
        setContent { StretchTheme(StretchThemes.Ink) { DiffViewer(listOf(file), Modifier.fillMaxSize()) } }
        onNodeWithText("notes.txt").performClick()
        waitForIdle()
        onAllNodesWithText("changed line").assertCountEquals(0)
    }
}
