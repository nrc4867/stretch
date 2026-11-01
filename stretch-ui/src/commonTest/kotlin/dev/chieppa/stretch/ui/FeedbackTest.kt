package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FeedbackTest {
    private val theme = StretchThemes.Ink

    @Test
    fun aButtonShowsRestHoverAndPressAsThreeDifferentFills() = runComposeUiTest {
        setContent { StretchTheme(theme) { Button("Duplicate", {}, Modifier.testTag("button")) } }
        fun fill(): Color = onNodeWithTag("button").captureToImage().toPixelMap().let { it[8, it.height / 2] }

        val rest = fill()
        onNodeWithTag("button").performMouseInput { moveTo(center) }
        val hover = fill()
        onNodeWithTag("button").performMouseInput { press() }
        val press = fill()
        onNodeWithTag("button").performMouseInput { release() }

        assertEquals(theme.colors.surfaceControl, rest)
        assertEquals(theme.colors.surfaceControlHi, hover)
        assertEquals(theme.colors.surfaceControlPress, press)
        assertNotEquals(hover, press)
    }

    @Test
    fun focusShowsAfterKeysAndNotAfterClicks() = runComposeUiTest {
        setContent {
            StretchTheme(theme) {
                Column(Modifier.focusHome().testTag("home")) {
                    Button("Export", {})
                    Box(Modifier.size(100.dp))
                }
            }
        }
        onNodeWithText("Export").performMouseInput {
            moveTo(center)
            press()
            release()
        }
        assertFalse(FocusVisibility.keyboard)
        onNodeWithTag("home").performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.Z) } }
        assertFalse(FocusVisibility.keyboard, "a shortcut is not keyboard navigation")
        onNodeWithTag("home").performKeyInput { pressKey(Key.Tab) }
        assertTrue(FocusVisibility.keyboard)
    }
}
