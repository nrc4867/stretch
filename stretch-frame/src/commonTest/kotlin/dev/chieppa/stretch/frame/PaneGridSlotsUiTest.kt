package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.IconButton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PaneGridSlotsUiTest {
    private val titles = mapOf("notes" to "Notes", "sketch" to "Sketch")

    private fun ComposeUiTest.showGrid(
        start: PaneLayout,
        tabMenu: (@Composable ColumnScope.(id: String, close: () -> Unit) -> Unit)? = null,
        paneActions: @Composable RowScope.(paneId: String) -> Unit = {},
    ): () -> PaneLayout {
        var layout by mutableStateOf(start)
        setStretchContent {
            Box(Modifier.size(800.dp, 400.dp)) {
                PaneGrid(layout, { layout = it }, { TabItem(it, titles.getValue(it)) }, tabMenu = tabMenu, paneActions = paneActions) { id ->
                    Box(Modifier.fillMaxSize().testTag("content $id"))
                }
            }
        }
        return { layout }
    }

    private fun ComposeUiTest.tab(title: String) = onNode(hasText(title) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

    private fun twoPanes() = PaneLayout().open("notes").open("sketch").splitWith("sketch", PaneLayout.FIRST_LEAF, PaneEdge.End)

    @Test
    fun eachPaneStripHasTheAppsActionsForThatPane() = runComposeUiTest {
        var acted: String? = null
        val layout = showGrid(twoPanes(), paneActions = { pane -> IconButton(StretchIcons.Close, "Close others in $pane", { acted = pane }) })
        val panes = layout().leaves.map { it.id }
        assertEquals(2, panes.size)
        val second = panes[1]
        onNodeWithContentDescription("Close others in $second").performClick()
        assertEquals(second, acted)
        val action = onNodeWithContentDescription("Close others in $second").getUnclippedBoundsInRoot()
        val sketch = tab("Sketch").getUnclippedBoundsInRoot()
        assertTrue(action.left >= sketch.right && action.top < sketch.bottom, "after the tabs, in the strip")
    }
}
