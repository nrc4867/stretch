package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.opensOnPress
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OpensOnPressTest {
    private fun androidx.compose.ui.test.ComposeUiTest.menuButton() = setStretchContent {
        var open by remember { mutableStateOf(false) }
        Box {
            Button("Open", { open = true }, Modifier.opensOnPress(open) { open = true })
            Menu(open, { open = false }) { BasicText("Item") }
        }
    }

    @Test
    fun theMouseOpensOnPressAndReleasingLeavesItOpen() = runComposeUiTest {
        menuButton()
        onNodeWithText("Item").assertDoesNotExist()
        onNodeWithText("Open").performMouseInput { press() }
        waitForIdle()
        onNodeWithText("Item").assertExists()
        onNodeWithText("Open").performMouseInput { release() }
        waitForIdle()
        onNodeWithText("Item").assertExists()
    }

    @Test
    fun pressingAgainClosesWithoutReopening() = runComposeUiTest {
        menuButton()
        onNodeWithText("Open").performMouseInput { press(); release() }
        waitForIdle()
        onNodeWithText("Item").assertExists()
        onNodeWithText("Open").performMouseInput { press() }
        waitForIdle()
        onNodeWithText("Item").assertDoesNotExist()
        onNodeWithText("Open").performMouseInput { release() }
        waitForIdle()
        onNodeWithText("Item").assertDoesNotExist()
    }

    @Test
    fun aTouchStillOpensOnItsClick() = runComposeUiTest {
        menuButton()
        onNodeWithText("Open").performClick()
        waitForIdle()
        onNodeWithText("Item").assertExists()
    }
}
