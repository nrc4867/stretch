package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SubMenuItemTest {
    @Test
    fun aSubmenuOpensBesideItsItemAndChoosingInItClosesTheWholeMenu() = runComposeUiTest {
        var ran = ""
        setStretchContent {
            ContextMenuArea({ close ->
                MenuItem("Open", { ran = "open"; close() })
                SubMenuItem("Copy") {
                    MenuItem("Link", { ran = "link"; close() })
                    MenuItem("Number", { ran = "number"; close() })
                }
            }) {
                Box(Modifier.size(300.dp).testTag("area"))
            }
        }
        onNodeWithTag("area").performMouseInput { rightClick(Offset(20f, 20f)) }
        onNodeWithText("Link").assertDoesNotExist()
        onNodeWithText("Copy").performClick()
        onNodeWithText("Number").performClick()
        assertEquals("number", ran)
        onNodeWithText("Copy").assertDoesNotExist()
        onNodeWithText("Number").assertDoesNotExist()
    }

    @Test
    fun hoveringAnotherItemClosesTheOpenSubmenu() = runComposeUiTest {
        setStretchContent {
            ContextMenuArea({ close ->
                SubMenuItem("Copy") { MenuItem("Link", close) }
                MenuItem("Open", close)
            }) {
                Box(Modifier.size(300.dp).testTag("area"))
            }
        }
        onNodeWithTag("area").performMouseInput { rightClick(Offset(20f, 20f)) }
        onNodeWithText("Copy").performClick()
        onNodeWithText("Link").assertExists()
        onNodeWithText("Open").performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithText("Link").assertDoesNotExist()
    }
}
