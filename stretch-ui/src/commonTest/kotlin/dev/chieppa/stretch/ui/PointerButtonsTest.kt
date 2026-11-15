package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PointerButtonsTest {
    @Test
    fun aMiddleClickClicksAndOtherButtonsDoNot() = runComposeUiTest {
        var clicks = 0
        setContent { Box(Modifier.size(100.dp).testTag("chip").onMiddleClick { clicks++ }) }
        onNodeWithTag("chip").performMouseInput {
            moveTo(center)
            press(MouseButton.Tertiary)
            release(MouseButton.Tertiary)
        }
        onNodeWithTag("chip").performMouseInput {
            press(MouseButton.Primary)
            release(MouseButton.Primary)
        }
        waitForIdle()
        assertEquals(1, clicks)
    }

    @Test
    fun theBackAndForwardButtonsGoBackAndForward() = runComposeUiTest {
        val heard = mutableListOf<String>()
        setContent { Box(Modifier.size(100.dp).testTag("window").onBackForwardButtons({ heard += "back" }, { heard += "forward" })) }
        onNodeWithTag("window").performMouseInput {
            moveTo(center)
            press(BACK)
            release(BACK)
            press(FORWARD)
            release(FORWARD)
            press(MouseButton.Primary)
            release(MouseButton.Primary)
        }
        waitForIdle()
        assertEquals(listOf("back", "forward"), heard)
    }

    private companion object {
        /** The mouse's fourth and fifth buttons, as Compose numbers them from 0. */
        val BACK = MouseButton(3)
        val FORWARD = MouseButton(4)
    }
}
