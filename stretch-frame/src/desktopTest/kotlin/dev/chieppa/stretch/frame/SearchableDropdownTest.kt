package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SearchableDropdownTest {
    private val bones = listOf("Head", "Neck", "Spine", "Tail base", "Tail tip")

    @Test
    fun searchNarrowsAndEnterPicksTheBestMatch() = runComposeUiTest {
        var bone by mutableStateOf("Head")
        setContent {
            StretchTheme(StretchThemes.Ink) {
                SearchableDropdown(bones, bone, { bone = it }, { it }, "Bone", Modifier.width(200.dp))
            }
        }
        onNodeWithContentDescription("Bone").performClick()
        val search = onNodeWithContentDescription("Search")
        search.performTextInput("tail")
        onNodeWithText("Neck").assertDoesNotExist()
        search.performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.Enter)
        }
        assertEquals("Tail tip", bone)
        search.assertDoesNotExist()
    }

    @Test
    fun clickingPicksAndEscapeLeavesItAlone() = runComposeUiTest {
        var bone by mutableStateOf("Head")
        setContent {
            StretchTheme(StretchThemes.Ink) {
                SearchableDropdown(bones, bone, { bone = it }, { it }, "Bone", Modifier.width(200.dp))
            }
        }
        onNodeWithContentDescription("Bone").performClick()
        onNodeWithText("Spine").performClick()
        assertEquals("Spine", bone)

        onNodeWithContentDescription("Bone").performClick()
        onNodeWithContentDescription("Search").performKeyInput { pressKey(Key.Escape) }
        assertEquals("Spine", bone)
        onNodeWithContentDescription("Search").assertDoesNotExist()
    }

    @Test
    fun noMatchesSaysSo() = runComposeUiTest {
        setContent {
            StretchTheme(StretchThemes.Ink) {
                SearchableDropdown(bones, "Head", {}, { it }, "Bone", Modifier.width(200.dp))
            }
        }
        onNodeWithContentDescription("Bone").performClick()
        val search = onNodeWithContentDescription("Search")
        search.performTextInput("zzz")
        onNodeWithText("No matches").assertExists()
        search.performKeyInput { pressKey(Key.Enter) }
        search.assertExists()
    }
}
