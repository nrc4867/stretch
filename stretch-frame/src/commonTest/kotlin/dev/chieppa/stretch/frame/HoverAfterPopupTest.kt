package dev.chieppa.stretch.frame

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test

/** Once a popup closes, the window under it shows hover again. */
@OptIn(ExperimentalTestApi::class)
class HoverAfterPopupTest {
    @Composable
    private fun Target(tag: String) {
        val source = remember { MutableInteractionSource() }
        val hovered by source.collectIsHoveredAsState()
        BasicText(if (hovered) "$tag hovered" else "$tag rest", Modifier.size(120.dp, 40.dp).testTag(tag).hoverable(source))
    }

    private fun ComposeUiTest.content(picker: @Composable () -> Unit) = setStretchContent {
        Column(Modifier.size(400.dp, 700.dp)) {
            picker()
            Spacer(Modifier.size(1.dp, 300.dp))
            Target("a")
            Target("b")
        }
    }

    private fun ComposeUiTest.clickOn(tag: String) {
        onNodeWithTag(tag).performMouseInput { moveTo(center); press(); release() }
        waitForIdle()
    }

    private fun ComposeUiTest.hoverWorks() {
        onNodeWithTag("a").performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithText("a hovered").assertExists()
        onNodeWithTag("b").performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithText("b hovered").assertExists()
        onNodeWithText("a rest").assertExists()
    }

    @Test
    fun aSearchableDropdownClosesOnAClickWellBelowItAndHoverComesBack() = runComposeUiTest {
        content { SearchableDropdown(listOf("One", "Two"), "One", {}, { it }, "Pick", Modifier.size(160.dp, 32.dp).testTag("picker")) }
        clickOn("picker")
        onNodeWithText("Two").assertExists()
        clickOn("b")
        onNodeWithText("Two").assertDoesNotExist()
        hoverWorks()
    }

    @Test
    fun aDropdownClosesOnAClickOutsideAndHoverComesBack() = runComposeUiTest {
        content { Dropdown(listOf("One", "Two"), "One", {}, { it }, "Pick", Modifier.size(160.dp, 32.dp).testTag("picker")) }
        clickOn("picker")
        onNodeWithText("Two").assertExists()
        clickOn("b")
        onNodeWithText("Two").assertDoesNotExist()
        hoverWorks()
    }
}
