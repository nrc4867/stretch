package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TabStripUiTest {
    private val long = "A rather long title for a view that will not fit in a tab"

    private fun ComposeUiTest.showStrip(tabs: List<TabItem>, width: Dp = 600.dp) {
        setStretchContent {
            Box(Modifier.width(width)) { TabStrip(tabs, tabs.first().id, {}, {}) }
        }
    }

    private fun ComposeUiTest.tab(title: String) =
        onNode(hasText(title) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab), useUnmergedTree = false)

    private fun ComposeUiTest.widthOf(title: String) = tab(title).getUnclippedBoundsInRoot().let { it.right - it.left }

    @Test
    fun aShortTitleMakesANarrowTab() = runComposeUiTest {
        showStrip(listOf(TabItem("app", "App.kt")))
        val width = widthOf("App.kt")
        assertTrue(width < 140.dp, "sized to its title: $width")
        assertTrue(width >= TabMinWidth, "never under the least width: $width")
    }

    @Test
    fun aLongTitleIsCappedAtTheGreatestWidth() = runComposeUiTest {
        showStrip(listOf(TabItem("long", long)))
        assertEquals(TabMaxWidth, widthOf(long))
    }

    @Test
    fun aCrowdedStripShrinksTheWidestTabsFirst() = runComposeUiTest {
        val tabs = listOf(TabItem("app", "App.kt")) + (1..3).map { TabItem("long $it", "$long $it") }
        showStrip(tabs)
        waitForIdle()
        val short = widthOf("App.kt")
        val longs = (1..3).map { widthOf("$long $it") }
        assertTrue(longs.all { it < TabMaxWidth && it > short }, "the long tabs gave way: $longs, short $short")
        assertTrue(short < 140.dp, "the short tab kept its width: $short")
        val right = tab("$long 3").getUnclippedBoundsInRoot().right
        assertTrue(right <= 601.dp, "everything fits without scrolling: $right")
    }

    @Test
    fun aStripTooCrowdedEvenAtTheLeastWidthScrolls() = runComposeUiTest {
        val tabs = (1..12).map { TabItem("long $it", "$long $it") }
        showStrip(tabs)
        waitForIdle()
        val widths = (1..3).map { widthOf("$long $it") }
        assertTrue(widths.all { it == TabMinWidth }, "every tab at the least width: $widths")
        assertTrue(tab("$long 12").getUnclippedBoundsInRoot().right > 600.dp, "the strip scrolls past its width")
    }

    @Test
    fun aSubtitleShowsAfterTheTitleAndIsHeard() = runComposeUiTest {
        showStrip(listOf(TabItem("plain", "App.kt"), TabItem("repo", "Main.kt", subtitle = "arc-review")))
        val tab = tab("Main.kt")
        tab.assert(hasText("arc-review"))
        assertTrue(widthOf("Main.kt") > widthOf("App.kt") + 40.dp, "the subtitle takes room after the title")
    }

    @Test
    fun aCrowdedTabStillTellsItsSubtitle() = runComposeUiTest {
        showStrip((1..8).map { TabItem("t$it", "File$it.kt", subtitle = "a-repository-with-a-long-name") }, width = 400.dp)
        waitForIdle()
        tab("File1.kt").assert(hasText("a-repository-with-a-long-name"))
        assertEquals(TabMinWidth, widthOf("File1.kt"))
    }

    @Test
    fun fittingKeepsWidthsThatFitAndCapsTheWidest() {
        assertEquals(listOf(100, 200), fitTabWidths(listOf(100, 200), 400, 50))
        assertEquals(listOf(100, 150, 150), fitTabWidths(listOf(100, 200, 240), 400, 50))
        assertEquals(listOf(50, 50, 50), fitTabWidths(listOf(100, 200, 240), 90, 50))
    }
}
