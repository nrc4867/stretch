package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NavigationComponentsTest {
    @Test
    fun pageWindowKeepsTheEndsAndTheNeighbours() {
        assertEquals(listOf<Int?>(0, 1, 2, 3), pageWindow(0, 4))
        assertEquals(listOf<Int?>(0, 1, null, 23), pageWindow(0, 24))
        assertEquals(listOf<Int?>(0, null, 4, 5, 6, null, 23), pageWindow(5, 24))
        assertEquals(listOf<Int?>(0, null, 22, 23), pageWindow(23, 24))
        assertEquals(listOf<Int?>(0), pageWindow(0, 1))
    }

    @Test
    fun aGapNeverHidesOnePage() {
        assertEquals(listOf<Int?>(0, 1, 2, 3, 4), pageWindow(3, 5))
    }

    @Test
    fun paginatorMovesAndStopsAtTheEnds() = runComposeUiTest {
        var page by mutableIntStateOf(0)
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Paginator(page, 3, { page = it }, "Previous", "Next")
            }
        }
        onNodeWithText("1").assertIsSelected()
        onNodeWithContentDescription("Previous").assertIsNotEnabled()
        onNodeWithContentDescription("Next").assertIsEnabled().performClick()
        assertEquals(1, page)
        onNodeWithText("3").performClick()
        assertEquals(2, page)
        onNodeWithContentDescription("Next").assertIsNotEnabled()
    }

    @Test
    fun stepperReportsTheCurrentStepAndGoesWhereItIsClicked() = runComposeUiTest {
        var step by mutableIntStateOf(1)
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Stepper(listOf("Draw", "Fit", "Join"), step, Modifier.width(400.dp), onStep = { step = it })
            }
        }
        onNodeWithText("Fit").assertIsSelected()
        onNodeWithText("Join").assertIsNotSelected().performClick()
        assertEquals(2, step)
        onNodeWithText("Join").assertIsSelected()
    }

    @Test
    fun aStepWithAProblemSaysSo() = runComposeUiTest {
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Stepper(listOf("Draw", "Fit"), 1, problems = mapOf(0 to "Ear missing"))
            }
        }
        onNodeWithText("Draw").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    @Test
    fun linkClicks() = runComposeUiTest {
        var clicks = 0
        setContent {
            StretchTheme(StretchThemes.Ink) { Link("Open library", { clicks++ }, external = true) }
        }
        onNodeWithText("Open library").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun splitButtonHalvesDoTheirOwnThing() = runComposeUiTest {
        var main = 0
        var open = 0
        setContent {
            StretchTheme(StretchThemes.Ink) { SplitButtonFace("Export", { main++ }, "More", { open++ }, kind = ButtonKind.Primary) }
        }
        onNodeWithText("Export").performClick()
        assertEquals(1 to 0, main to open)
        onNodeWithContentDescription("More").performClick()
        assertEquals(1 to 1, main to open)
    }
}
