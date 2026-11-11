package dev.chieppa.stretch.diff

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.code.CodeRange
import dev.chieppa.stretch.code.SYMBOL_CARD_DELAY_MS
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DiffSymbolCardTest {
    private val added = diffTexts("", (1..40).joinToString("\n") { "row $it of a file that was added whole" }, null, "added.txt")

    @Test
    fun restingOnASymbolOpensItsCardWithTheSide() = runComposeUiTest {
        mainClock.autoAdvance = false
        val slots = DiffSlots(
            symbolAt = { CodeRange(it.line, 0, 3) },
            onSymbolClick = {},
            symbolCard = { position, _, _ -> BasicText("card ${position.side} ${position.line}") },
        )
        setStretchContent { DiffViewer(listOf(added), Modifier.size(700.dp, 600.dp).testTag("diff"), slots = slots) }
        waitForIdle()
        val row = onNodeWithText("row 5 of a file that was added whole", substring = true, useUnmergedTree = true)
        row.performMouseInput { moveTo(Offset(4f, center.y)) }
        mainClock.advanceTimeBy(SYMBOL_CARD_DELAY_MS * 2)
        waitForIdle()
        onNodeWithText("card New 5").assertExists()
    }
}
