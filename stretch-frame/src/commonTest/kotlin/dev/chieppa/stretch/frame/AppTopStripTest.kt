package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchIcons
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AppTopStripTest {
    @Test
    fun theTopStripSpansTheWindowBetweenTheBarAndTheFrame() = runComposeUiTest {
        val drawers = listOf(DrawerSpec("layers", "Layers", StretchIcons.Layers) { RowText("Body") })
        setStretchContent {
            Box(Modifier.size(1000.dp, 700.dp).testTag("app")) {
                StretchApp(
                    FrameLayout(listOf(DrawerPlacement("layers", Side.Left, open = true))),
                    {},
                    drawers,
                    onResetLayout = {},
                    topStrip = { Box(Modifier.fillMaxWidth().height(32.dp).testTag("strip")) },
                    topBar = { Box(Modifier.testTag("bar")) { TopBar("App", "Untitled") } },
                ) { Box(Modifier.fillMaxSize().testTag("viewport")) }
            }
        }
        val app = onNodeWithTag("app").getUnclippedBoundsInRoot()
        val bar = onNodeWithTag("bar").getUnclippedBoundsInRoot()
        val strip = onNodeWithTag("strip").getUnclippedBoundsInRoot()
        val rail = onNodeWithContentDescription("Layers").getUnclippedBoundsInRoot()
        val drawer = onNodeWithText("Body").getUnclippedBoundsInRoot()
        val viewport = onNodeWithTag("viewport").getUnclippedBoundsInRoot()
        assertEquals(bar.bottom, strip.top, "right under the bar")
        assertEquals(app.left, strip.left)
        assertEquals(app.right, strip.right, "across the whole window")
        assertEquals(strip.bottom, viewport.top, "the viewport starts under it")
        assertTrue(rail.top >= strip.bottom, "the rails start under it")
        assertTrue(drawer.top >= strip.bottom, "the drawers start under it")
    }
}
