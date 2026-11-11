package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CodeBlockTest {
    private val text = CodeText.of((1..8).joinToString("\n") { "line $it" })

    @Test
    fun aBlockDrawsInPlaceOfItsLinesAndKeepsTheirAnnotations() = runComposeUiTest {
        val slots = CodeViewerSlots(
            lineAnnotation = { line -> if (line == 4) BasicText("note on 4") },
            lineBlock = { lines -> BasicText("block ${lines.first}-${lines.last}") },
        )
        setStretchContent { CodeViewer(text, Modifier.size(600.dp, 400.dp), slots = slots, blocks = listOf(3..5)) }
        waitForIdle()
        onNodeWithText("block 3-5").assertExists()
        onNodeWithText("line 2").assertExists()
        onNodeWithText("line 4").assertDoesNotExist()
        onNodeWithText("line 6").assertExists()
        onNodeWithText("note on 4").assertExists()
    }

    @Test
    fun overlappingAndOutOfRangeBlocksAreLeftOut() {
        val at = CodeBlocks.byLine(listOf(2..4, 3..6, 7..12, 6..6), lineCount = 8)
        assertEquals(setOf(2, 3, 4, 6), at.keys)
        assertEquals(6..6, at[6])
    }
}
