package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CodeRulerTest {
    private val text = CodeText((1..1000).map { "line $it" })

    @Test
    fun pressingAndDraggingOnTheRulerScrolls() = runComposeUiTest {
        val state = CodeViewerState()
        setStretchContent(StretchThemes.Ink) { CodeViewer(text, Modifier.size(600.dp, 400.dp), state, tones = { if (it == 900) LineTone.Added else null }) }
        onRoot().performTouchInput {
            val x = width - 5.dp.toPx()
            down(Offset(x, height * 0.9f))
        }
        waitForIdle()
        assertTrue(state.firstVisibleLine in 850..900, "pressing near the bottom scrolls near line 900, not ${state.firstVisibleLine}")
        onRoot().performTouchInput {
            moveTo(Offset(width - 5.dp.toPx(), height * 0.5f))
            up()
        }
        waitForIdle()
        assertTrue(state.firstVisibleLine in 450..500, "dragging to the middle scrolls to the middle, not ${state.firstVisibleLine}")
    }
}
