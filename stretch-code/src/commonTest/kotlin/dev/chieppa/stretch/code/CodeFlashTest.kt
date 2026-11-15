package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CodeFlashTest {
    private val text = CodeText.of((1..60).joinToString("\n") { "val line$it = $it" })

    @Test
    fun aFlashShowsAtOnceThenFadesAndIsForgotten() = runComposeUiTest {
        val state = CodeViewerState()
        setStretchContent(StretchThemes.InkDark) { CodeViewer(text, Modifier.size(600.dp, 300.dp), state) }
        mainClock.autoAdvance = false
        state.flash(CodeRange(3, 4, 9))
        mainClock.advanceTimeByFrame()
        assertEquals(CodeRange(3, 4, 9), state.flashing)
        assertEquals(1f, state.flash.strength.value)
        mainClock.advanceTimeBy((CodeFlash.HOLD_MS + CodeFlash.FADE_MS / 2).toLong())
        assertTrue(state.flash.strength.value in 0.01f..0.99f)
        mainClock.advanceTimeBy(CodeFlash.FADE_MS.toLong())
        assertNull(state.flashing)
    }

    @Test
    fun aWholeLineFlashSpansTheLine() = runComposeUiTest {
        val state = CodeViewerState()
        setStretchContent(StretchThemes.InkDark) { CodeViewer(text, Modifier.size(600.dp, 300.dp), state) }
        state.flashLine(5)
        assertEquals(CodeRange(5, 0, Int.MAX_VALUE), state.flashing)
    }

    @Test
    fun rowBoxesCoverARunAndAPlaceBetweenCharacters() = runComposeUiTest {
        val geometry = CodeLineGeometry("\tval x = 1")
        setStretchContent(StretchThemes.InkDark) {
            CodeLineBody(androidx.compose.ui.text.AnnotatedString(expandTabs(geometry.text)), rememberCodeMetrics(20, 1), rememberCodeLook(), wrap = false, scroll = androidx.compose.foundation.rememberScrollState(), onTextLayout = { geometry.layout = it })
        }
        val layout = geometry.layout!!
        val run = geometry.rowBoxes(1, 4, layout).single()
        assertTrue(run.width > 0f && run.left > 0f)
        val caret = geometry.rowBoxes(2, 2, layout).single()
        assertTrue(caret.width > 0f)
    }
}
