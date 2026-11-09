package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
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
class NestedContextMenuTest {
    @Test
    fun anAreaInsideAnotherOpensOneMenuWithBothItsItemsAndTheOuterOnes() = runComposeUiTest {
        var ran = ""
        setStretchContent {
            ContextMenuArea({ close -> MenuItem("Hide card", { ran = "hide"; close() }) }) {
                Box(Modifier.size(300.dp).testTag("card")) {
                    ContextMenuArea({ close -> MenuItem("Remove row", { ran = "remove"; close() }) }) {
                        Box(Modifier.size(100.dp).testTag("row"))
                    }
                }
            }
        }
        onNodeWithTag("row").performMouseInput { rightClick(Offset(20f, 20f)) }
        waitForIdle()
        assertEquals(1, onAllNodesWithText("Remove row").fetchSemanticsNodes().size)
        assertEquals(1, onAllNodesWithText("Hide card").fetchSemanticsNodes().size)
        onNodeWithText("Remove row").performClick()
        waitForIdle()
        assertEquals("remove", ran)
        onNodeWithText("Hide card").assertDoesNotExist()

        onNodeWithTag("card").performMouseInput { rightClick(Offset(250f, 250f)) }
        waitForIdle()
        onNodeWithText("Hide card").assertExists()
        onNodeWithText("Remove row").assertDoesNotExist()
    }
}
