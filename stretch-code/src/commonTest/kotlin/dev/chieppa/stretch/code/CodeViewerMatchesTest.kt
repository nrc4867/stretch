package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchThemes
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CodeViewerMatchesTest {
    private val text = CodeText((1..2000).map { "line $it of the file" })
    private val matches = listOf(CodeRange(3, 0, 4), CodeRange(1500, 5, 9))

    @Test
    fun drawsTheAppsMatchesAndScrollsToOne() = runComposeUiTest {
        val state = CodeViewerState()
        var look: CodeLook? = null
        setStretchContent(StretchThemes.Ink) {
            look = rememberCodeLook()
            CodeViewer(text, Modifier.size(600.dp, 400.dp), state, matches = matches, currentMatch = matches[1])
        }
        assertEquals(listOf(look!!.match), onNodeWithText("line 3 of the file").spanBackgrounds().filter { it != Color.Unspecified })
        onNodeWithText("line 1500 of the file").assertDoesNotExist()
        runOnIdle { runBlocking { state.scrollToMatch(matches[1]) } }
        waitForIdle()
        assertTrue(look!!.currentMatch in onNodeWithText("line 1500 of the file").spanBackgrounds())
        assertTrue(state.firstVisibleLine in 1400..1500)
    }
}
