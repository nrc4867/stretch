package dev.chieppa.stretch.gallery

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test

/** The gallery's two pages render in every theme, and the frame demo's undo works through the top bar. */
@OptIn(ExperimentalTestApi::class)
class GalleryTest {
    @Test
    fun bothPagesRenderInEveryTheme() = runComposeUiTest {
        setContent { Gallery() }
        for (theme in StretchThemes.all) {
            onNodeWithText(theme.name).performClick()
            onNodeWithText("BUTTONS").assertExists()
        }
        onNodeWithText("App frame").performClick()
        for (theme in StretchThemes.all) {
            onNodeWithText(theme.name).performClick()
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
