package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchIcons
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DrawerLazyContentTest {
    private val composed = mutableSetOf<Int>()

    private fun list(count: Int) = DrawerSpec("files", "Files", StretchIcons.Layers, scrolls = false) {
        RowText("Header")
        LazyColumn(Modifier.weight(1f)) {
            items(count) { i ->
                SideEffect { composed += i }
                RowText("File $i")
            }
        }
    }

    private fun ComposeUiTest.showApp(spec: DrawerSpec, layout: FrameLayout, windows: DrawerWindowHost? = null) {
        setStretchContent {
            CompositionLocalProvider(LocalDrawerWindows provides windows) {
                Box(Modifier.size(1000.dp, 700.dp)) {
                    StretchApp(layout, {}, listOf(spec), onResetLayout = {}, topBar = { TopBar("App", "Untitled") }) { Viewport {} }
                }
            }
        }
        waitForIdle()
    }

    @Test
    fun aDrawerThatScrollsItselfBuildsOnlyTheRowsOnScreen() = runComposeUiTest {
        showApp(list(ROWS), FrameLayout(listOf(DrawerPlacement("files", Side.Left, open = true))))
        assertTrue(composed.isNotEmpty() && composed.size < SCREENFUL, "built ${composed.size} rows")
    }

    @Test
    fun aDrawerInItsOwnWindowThatScrollsItselfBuildsOnlyTheRowsOnScreen() = runComposeUiTest {
        val layout = FrameLayout(listOf(DrawerPlacement("files", Side.Left, open = true))).popOut("files")
        showApp(list(ROWS), layout, InlineWindows())
        onNodeWithTag("window").assertExists()
        assertTrue(composed.isNotEmpty() && composed.size < SCREENFUL, "built ${composed.size} rows")
    }

    /** Draws a drawer's window inline, a fixed size. */
    private class InlineWindows : DrawerWindowHost {
        override val dockTarget: DockTarget? = null

        override fun railBounds(side: Side, bounds: Rect?) {}

        override fun screenPosition(point: Offset) = point

        @Composable
        override fun DragFloat(at: Offset?, size: Dp, content: @Composable () -> Unit) {}

        @Composable
        override fun DrawerWindow(
            title: String,
            bounds: WindowBounds?,
            side: Side,
            defaultWidth: Float,
            raise: Int,
            onBoundsChange: (WindowBounds) -> Unit,
            onClose: () -> Unit,
            onDock: (side: Side, bottom: Boolean) -> Unit,
            content: @Composable () -> Unit,
        ) {
            Box(Modifier.size(300.dp, 400.dp).testTag("window")) { Box(Modifier.fillMaxSize()) { content() } }
        }
    }

    private companion object {
        const val ROWS = 10_000
        const val SCREENFUL = 60
    }
}
