package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.ui.Button
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PopoverScrollTest {
    private fun scrollRanges(test: androidx.compose.ui.test.ComposeUiTest) =
        test.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .fetchSemanticsNodes().map { it.config[SemanticsProperties.VerticalScrollAxisRange].maxValue() }

    @Test
    fun aPopoverPastItsMaxHeightScrollsItsContentUnderAPinnedTitleAndFooter() = runDesktopComposeUiTest(width = 500, height = 800) {
        var done = 0
        setStretchContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.TopStart).size(40.dp)) {
                    Popover(
                        open = true, onDismiss = {}, title = "Parts", maxHeight = 240.dp,
                        footer = { Button("Done", { done++ }) },
                    ) {
                        for (n in 1..30) BasicText("Row $n", Modifier.size(200.dp, 24.dp).testTag("row"))
                    }
                }
            }
        }
        waitForIdle()
        val title = onNodeWithText("Parts").getBoundsInRoot()
        val footer = onNodeWithText("Done").getBoundsInRoot()
        assertTrue(footer.bottom - title.top <= 240.dp, "title to footer spans ${footer.bottom - title.top}")
        onNodeWithText("Done").assertIsDisplayed()
        assertTrue(scrollRanges(this).any { it > 0f })
        onNodeWithText("Row 30").performScrollTo().assertIsDisplayed()
        assertEquals(title, onNodeWithText("Parts").getBoundsInRoot())
        assertEquals(footer, onNodeWithText("Done").getBoundsInRoot())
        onNodeWithText("Done").performClick()
        assertEquals(1, done)
    }

    @Test
    fun aPopoverTallerThanTheWindowStaysInsideIt() = runDesktopComposeUiTest(width = 500, height = 300) {
        setStretchContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.Center).size(40.dp)) {
                    Popover(open = true, onDismiss = {}, footer = { BasicText("Footer") }) {
                        for (n in 1..30) BasicText("Row $n", Modifier.size(200.dp, 24.dp))
                    }
                }
            }
        }
        waitForIdle()
        val footer = onNodeWithText("Footer").getBoundsInRoot()
        assertTrue(footer.top >= 0.dp && footer.bottom <= 300.dp)
        assertTrue(onNodeWithText("Row 1").getBoundsInRoot().top >= 0.dp)
    }

    @Test
    fun aPopoverThatFitsShowsAllOfItWithoutScrolling() = runDesktopComposeUiTest(width = 500, height = 800) {
        setStretchContent {
            Box(Modifier.size(40.dp)) {
                Popover(open = true, onDismiss = {}, title = "Brush", maxHeight = 400.dp) {
                    BasicText("Width")
                    BasicText("Taper")
                }
            }
        }
        waitForIdle()
        onNodeWithText("Width").assertIsDisplayed()
        onNodeWithText("Taper").assertIsDisplayed()
        assertTrue(scrollRanges(this).all { it == 0f })
    }
}
