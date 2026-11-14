package dev.chieppa.stretch.frame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class HeldTabUiTest {
    private val holds = TabHolds()
    private val tabs = WindowTabs(holds)

    private val reloaded = mutableListOf<String>()

    private fun ComposeUiTest.showGrid(start: PaneLayout, content: @Composable (String) -> Unit = { RowText("Showing $it") }): (PaneLayout) -> Unit {
        var layout by mutableStateOf(start)
        setStretchContent {
            CompositionLocalProvider(LocalTabHolds provides holds, LocalWindowTabs provides tabs) {
                Box(Modifier.size(800.dp, 500.dp)) {
                    PaneGrid(layout, { layout = it }, { TabItem(it, it.uppercase()) }, onReload = { reloaded += it }) { id -> content(id) }
                }
            }
        }
        return { layout = it }
    }

    @Test
    fun aHeldTabShowsItsCrashAndAReloadInPlaceOfItsContent() = runComposeUiTest {
        holds.hold("notes", "IllegalStateException: no section")
        showGrid(PaneLayout().open("notes"))
        onNodeWithText("Showing notes").assertDoesNotExist()
        onNodeWithText("IllegalStateException: no section").assertExists()
        onNodeWithContentDescription("Reload").performClick()
        onNodeWithText("Showing notes").assertExists()
        assertEquals(emptySet(), holds.ids)
        assertEquals(listOf("notes"), reloaded)
        assertEquals("notes", holds.lastReload?.first)
    }

    @Test
    fun aReloadStartsTheTabWithoutItsSavedState() = runComposeUiTest {
        showGrid(PaneLayout().open("notes")) {
            var count by rememberSaveable { mutableIntStateOf(0) }
            BasicText("Count $count", Modifier.clickable { count++ })
        }
        onNodeWithText("Count 0").performClick()
        onNodeWithText("Count 1").assertExists()
        runOnIdle { holds.hold("notes", "crashed") }
        onNodeWithText("crashed").assertExists()
        onNodeWithContentDescription("Reload").performClick()
        onNodeWithText("Count 0").assertExists()
    }

    @Test
    fun aTabStartingToShowIsNotedOnceTheWindowHasSettled() = runComposeUiTest {
        val change = showGrid(PaneLayout().open("notes").open("sketch").activate("notes"))
        waitForIdle()
        assertNull(tabs.lastShown)
        assertEquals(listOf("notes"), tabs.showing())

        tabs.settling = false
        change(PaneLayout().open("notes").open("sketch").activate("sketch"))
        waitForIdle()
        assertEquals("sketch", tabs.lastShown?.first)
        assertEquals(listOf("sketch"), tabs.showing())
    }
}
