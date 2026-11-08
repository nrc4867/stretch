package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ScrollbarTest {
    @Test
    fun aThumbShowsOnlyWhenThereIsSomethingToScroll() = runComposeUiTest {
        var count by mutableIntStateOf(3)
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Box(Modifier.size(100.dp, 300.dp)) {
                    val list = rememberLazyListState()
                    LazyColumn(Modifier.fillMaxSize(), list) {
                        stickyHeader { Box(Modifier.height(30.dp)) }
                        items(count) { Box(Modifier.height(if (it % 2 == 0) 20.dp else 60.dp)) }
                    }
                    Scrollbar(list, Modifier.align(Alignment.CenterEnd).testTag("bar"))
                }
            }
        }
        fun drawn(): Boolean = onNodeWithTag("bar").captureToImage().toPixelMap().let { pixels ->
            (0 until pixels.width).any { x -> (0 until pixels.height).any { y -> pixels[x, y].alpha > 0f } }
        }
        assertFalse(drawn())
        count = 100
        waitForIdle()
        assertTrue(drawn())
    }
}
