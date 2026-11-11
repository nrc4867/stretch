package dev.chieppa.stretch.diff

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DiffBlockTest {
    private val old = (1..10).joinToString("\n") { "row $it" }
    private val new = old.replace("row 8", "row eight")
    private val diff = diffTexts(old, new, "a.txt", "a.txt")
    private val slots = DiffSlots(lineBlock = { block, side -> BasicText("block ${block.lines.first}-${block.lines.last} $side") })

    @Test
    fun anUnchangedRunDrawsAsABlockOnEachSide() = runComposeUiTest {
        setStretchContent { DiffViewer(listOf(diff), Modifier.size(900.dp, 600.dp), DiffViewerState(split = true), slots = slots, blocks = listOf(DiffBlock(0, 5..6))) }
        waitForIdle()
        onNodeWithText("block 5-6 Old").assertExists()
        onNodeWithText("block 5-6 New").assertExists()
        assertEquals(0, onAllNodesWithText("row 6", substring = false).fetchSemanticsNodes().size)
    }

    @Test
    fun aRunTouchingAChangedLineDrawsAsCode() = runComposeUiTest {
        setStretchContent { DiffViewer(listOf(diff), Modifier.size(900.dp, 600.dp), DiffViewerState(split = false), slots = slots, blocks = listOf(DiffBlock(0, 7..8))) }
        waitForIdle()
        onNodeWithText("block 7-8 New").assertDoesNotExist()
        onNodeWithText("row 7").assertExists()
    }
}
