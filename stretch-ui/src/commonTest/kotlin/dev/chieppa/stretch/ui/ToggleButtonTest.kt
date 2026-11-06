package dev.chieppa.stretch.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ToggleButtonTest {
    @Test
    fun aClickTurnsItOnAndOff() = runComposeUiTest {
        var reviewed by mutableStateOf(false)
        setContent { StretchTheme(StretchThemes.Ink) { ToggleButton("Mark reviewed", reviewed, { reviewed = it }, icon = StretchIcons.Check) } }
        onNodeWithText("Mark reviewed", useUnmergedTree = true).assertExists()
        onNodeWithText("Mark reviewed").assertIsOff().performClick()
        onNodeWithText("Mark reviewed").assertIsOn().performClick()
        onNodeWithText("Mark reviewed").assertIsOff()
    }

    @Test
    fun aCompactOneTakesTheGivenHeight() = runComposeUiTest {
        setContent { StretchTheme(StretchThemes.Ink) { ToggleButton("Pin", true, {}, height = StretchSizes.seg) } }
        onNodeWithText("Pin").assertHeightIsEqualTo(StretchSizes.seg)
    }
}
