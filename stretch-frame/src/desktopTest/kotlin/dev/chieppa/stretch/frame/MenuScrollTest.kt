package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MenuScrollTest {
    private val windowHeight = 300

    @Test
    fun aMenuTallerThanTheWindowScrollsToItsLastItemAndClicksIt() = runDesktopComposeUiTest(width = 400, height = windowHeight) {
        var chosen = ""
        setStretchContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.TopStart)) {
                    Menu(expanded = true, onDismiss = {}) {
                        for (n in 1..40) MenuItem("Item $n", { chosen = "Item $n" })
                    }
                }
            }
        }
        waitForIdle()
        onNodeWithText("Item 40").performScrollTo().performClick()
        assertEquals("Item 40", chosen)
    }

    @Test
    fun aTallMenuStaysInsideTheWindow() = runDesktopComposeUiTest(width = 400, height = windowHeight) {
        setStretchContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.Center)) {
                    Menu(expanded = true, onDismiss = {}) {
                        for (n in 1..40) MenuItem("Item $n", {})
                    }
                }
            }
        }
        waitForIdle()
        val first = onNodeWithText("Item 1").getBoundsInRoot()
        assertTrue(first.top >= 0.dp)
        assertTrue(first.bottom <= windowHeight.dp)
    }

    @Test
    fun aShortMenuShowsEveryItemWithoutScrolling() = runDesktopComposeUiTest(width = 400, height = 600) {
        setStretchContent {
            Box(Modifier.size(40.dp)) {
                Menu(expanded = true, onDismiss = {}) {
                    for (n in 1..3) MenuItem("Item $n", {})
                }
            }
        }
        waitForIdle()
        onNodeWithText("Item 1").assertIsDisplayed()
        onNodeWithText("Item 3").assertIsDisplayed()
        val scroll = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .fetchSemanticsNodes().map { it.config[SemanticsProperties.VerticalScrollAxisRange] }
        assertTrue(scroll.all { it.maxValue() == 0f })
    }

    @Test
    fun aTallMenuIsDrawnOnlyAtItsFinalPlace() = runDesktopComposeUiTest(width = 400, height = windowHeight) {
        var open by mutableStateOf(false)
        var top = 0f
        val drawnAt = mutableListOf<Float>()
        setStretchContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.BottomStart).padding(bottom = 40.dp).size(40.dp)) {
                    Menu(expanded = open, onDismiss = {}) {
                        Box(Modifier.size(8.dp).onGloballyPositioned { top = it.positionInWindow().y }.drawBehind { drawnAt += top })
                        for (n in 1..40) MenuItem("Item $n", {})
                    }
                }
            }
        }
        open = true
        waitForIdle()
        onNodeWithText("Item 1").assertExists()
        assertTrue(drawnAt.isNotEmpty())
        assertEquals(listOf(drawnAt.last()), drawnAt.distinct())
    }
}
