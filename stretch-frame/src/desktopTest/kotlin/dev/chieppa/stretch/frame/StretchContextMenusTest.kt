package dev.chieppa.stretch.frame

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class StretchContextMenusTest {
    private val stretchItem = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    @Test
    fun selectableTextOpensStretchsMenu() = runDesktopComposeUiTest {
        setStretchContent {
            StretchContextMenus { SelectionContainer { BasicText("Some selectable text") } }
        }
        onNodeWithText("Some selectable text").performMouseInput { rightClick() }
        waitForIdle()
        onNode(hasText("Copy", ignoreCase = true) and stretchItem).assertExists()
    }
}
