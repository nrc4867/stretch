package dev.chieppa.stretch.frame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TitleBarHitTestTest {
    @Test
    fun onlyMarkedControlsKeepThePointerFromTheOs() = runComposeUiTest {
        val hits = mutableListOf<Boolean>()
        var clicks = 0
        setStretchContent {
            val regions = TitleBarRegions()
            Column {
                Spacer(Modifier.height(30.dp))
                Row(Modifier.titleBarHitTest(regions) { hits += it }.testTag("bar")) {
                    Box(Modifier.size(40.dp).testTag("name"))
                    Box(Modifier.size(40.dp).titleBarControl(regions, "button").clickable { clicks++ }.testTag("button"))
                    Spacer(Modifier.width(200.dp))
                }
            }
        }
        onNodeWithTag("bar").performMouseInput { moveTo(Offset(20f, 20f)) }
        assertEquals(false, hits.last())
        onNodeWithTag("button").performMouseInput { moveTo(center) }
        assertEquals(true, hits.last())
        onNodeWithTag("bar").performMouseInput { moveTo(Offset(width - 10f, 20f)) }
        assertEquals(false, hits.last())
        onNodeWithTag("button").performMouseInput {
            moveTo(center)
            press()
            release()
        }
        assertTrue(hits.last())
        assertEquals(1, clicks)
    }
}
