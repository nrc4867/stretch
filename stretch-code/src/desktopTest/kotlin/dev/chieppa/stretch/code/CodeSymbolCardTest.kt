package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.click
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CodeSymbolCardTest {
    private val name = "a".repeat(40)
    private val text = CodeText.of("$name\n${"b".repeat(40)}")
    private val overName = Offset(200f, 8f)
    private val overOther = Offset(200f, 26f)

    private fun ComposeUiTest.show(pinOnOpen: Boolean = false) {
        mainClock.autoAdvance = false
        val slots = CodeViewerSlots(
            symbolAt = { if (it.line == 1) CodeRange(1, 0, name.length) else null },
            onSymbolClick = {},
            symbolCard = { position, range, card ->
                if (pinOnOpen) card.pinned = true
                BasicText("card ${position.line}:${range.start}-${range.end}")
            },
        )
        setStretchContent { CodeViewer(text, Modifier.size(700.dp, 300.dp).testTag("code"), slots = slots) }
    }

    private fun ComposeUiTest.rest(at: Offset, millis: Long) {
        onNodeWithTag("code").performMouseInput { moveTo(at) }
        mainClock.advanceTimeBy(millis)
        waitForIdle()
    }

    @Test
    fun restingOnASymbolOpensItsCard() = runComposeUiTest {
        show()
        rest(overName, SYMBOL_CARD_DELAY_MS / 2)
        onNodeWithText("card 1:0-40").assertDoesNotExist()
        mainClock.advanceTimeBy(SYMBOL_CARD_DELAY_MS)
        waitForIdle()
        onNodeWithText("card 1:0-40").assertExists()
    }

    @Test
    fun leavingTheSymbolClosesTheCardAfterAGrace() = runComposeUiTest {
        show()
        rest(overName, SYMBOL_CARD_DELAY_MS * 2)
        rest(overOther, SYMBOL_CARD_GRACE_MS / 2)
        onNodeWithText("card 1:0-40").assertExists()
        mainClock.advanceTimeBy(SYMBOL_CARD_GRACE_MS)
        waitForIdle()
        onNodeWithText("card 1:0-40").assertDoesNotExist()
    }

    @Test
    fun aPinnedCardStaysWhenThePointerLeaves() = runComposeUiTest {
        show(pinOnOpen = true)
        rest(overName, SYMBOL_CARD_DELAY_MS * 2)
        rest(overOther, SYMBOL_CARD_GRACE_MS * 2)
        onNodeWithText("card 1:0-40").assertExists()
    }

    @Test
    fun escapeClosesTheCard() = runComposeUiTest {
        show(pinOnOpen = true)
        rest(overName, SYMBOL_CARD_DELAY_MS * 2)
        onNodeWithTag("code").performMouseInput { click(overOther) }
        onNodeWithTag("code").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        onNodeWithText("card 1:0-40").assertDoesNotExist()
    }
}
