package dev.chieppa.stretch.gallery

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test

/** The gallery's three pages render in every theme, and the frame demo's undo works through the top bar. */
@OptIn(ExperimentalTestApi::class)
class GalleryTest {
    private fun ComposeUiTest.pickTheme(name: String) {
        onNodeWithContentDescription("Theme").performClick()
        onAllNodesWithText(name).onLast().performClick()
    }

    @Test
    fun everyPageRendersInEveryTheme() = runComposeUiTest {
        setContent { Gallery() }
        for (theme in StretchThemes.all) {
            pickTheme(theme.name)
            onNodeWithText("BUTTONS").assertExists()
        }
        onNodeWithText("Data").performClick()
        for (theme in StretchThemes.all) {
            pickTheme(theme.name)
            onNodeWithText("Strokes per day").assertExists()
        }
        onNodeWithText("App frame").performClick()
        for (theme in StretchThemes.all) {
            pickTheme(theme.name)
            onAllNodesWithText("Layers").onFirst().assertExists()
        }
    }

    @Test
    fun deletingALayerCanBeUndone() = runComposeUiTest {
        setContent { Gallery() }
        onNodeWithText("App frame").performClick()
        onNodeWithText("Tail").performClick()
        onNodeWithText("Delete layer").performClick()
        onNodeWithText("Tail").assertDoesNotExist()
        onNodeWithContentDescription("Undo").performClick()
        onNodeWithText("Tail").assertExists()
    }
}
