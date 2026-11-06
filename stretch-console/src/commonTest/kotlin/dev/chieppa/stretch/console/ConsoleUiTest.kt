package dev.chieppa.stretch.console

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ConsoleUiTest {
    @Test
    fun followsNewOutputUntilScrolledBack() = runComposeUiTest {
        val state = ConsoleState()
        setContent { StretchTheme(StretchThemes.InkDark) { Console(state, Modifier.size(500.dp, 300.dp)) } }
        runOnIdle { repeat(500) { state.append("output line $it\n") } }
        waitForIdle()
        onNodeWithText("output line 499").assertExists()
        onNodeWithText("output line 499").performMouseInput { scroll(-2000f) }
        waitForIdle()
        assertFalse(state.follow)
        runOnIdle { state.append("output line 500\n") }
        waitForIdle()
        onNodeWithText("output line 500").assertDoesNotExist()
        onNodeWithText("Jump to the end").performClick()
        waitForIdle()
        assertTrue(state.follow)
        onNodeWithText("output line 500").assertExists()
    }

    @Test
    fun reportsClickedLinks() = runComposeUiTest {
        val state = ConsoleState()
        var clicked: ConsoleLink? = null
        setContent { StretchTheme(StretchThemes.Ink) { Console(state, Modifier.size(600.dp, 200.dp), onLinkClick = { clicked = it }) } }
        runOnIdle { state.append("src/App.kt:7:3: error\n") }
        waitForIdle()
        onNodeWithText("src/App.kt:7:3: error").performMouseInput { click(centerLeft.copy(x = 20f)) }
        assertEquals(ConsoleLink(0, 14, "src/App.kt", 7, 3), clicked)
    }

    @Test
    fun findsAndCounts() = runComposeUiTest {
        val state = ConsoleState()
        setContent { StretchTheme(StretchThemes.Ink) { Console(state, Modifier.size(600.dp, 300.dp)) } }
        runOnIdle {
            state.append("first warning\nnothing\nsecond warning\n")
            state.findOpen = true
        }
        waitForIdle()
        onNodeWithContentDescription("Find").performTextInput("warning")
        waitForIdle()
        onNodeWithText("1 of 2").assertExists()
        onNodeWithContentDescription("Find").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        onNodeWithText("2 of 2").assertExists()
        runOnIdle { state.clear() }
        waitForIdle()
        assertEquals(0, state.matchCount)
    }
}
