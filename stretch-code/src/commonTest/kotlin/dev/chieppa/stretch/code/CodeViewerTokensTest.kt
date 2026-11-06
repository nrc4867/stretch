package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchThemes
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The colors spans of a line's text draw in. */
internal fun SemanticsNodeInteraction.spanColors(): List<Color> =
    fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text).orEmpty().flatMap { text -> text.spanStyles.map { it.item.color } }

/** The backgrounds spans of a line's text draw on. */
internal fun SemanticsNodeInteraction.spanBackgrounds(): List<Color> =
    fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text).orEmpty().flatMap { text -> text.spanStyles.map { it.item.background } }

@OptIn(ExperimentalTestApi::class)
class CodeViewerTokensTest {
    private val text = CodeText((1..2000).map { "plain line $it" })

    @Test
    fun colorsFromSuppliedTokensThatArriveLaterAndKeepsItsPlace() = runComposeUiTest {
        val state = CodeViewerState()
        var tokens by mutableStateOf<List<List<CodeToken>>?>(null)
        var keyword = Color.Unspecified
        setStretchContent(StretchThemes.Ink) {
            keyword = codeColors.keyword
            CodeViewer(text, Modifier.size(600.dp, 400.dp), state, tokens = tokens)
        }
        runOnIdle { runBlocking { state.alignLine(1200) } }
        waitForIdle()
        assertEquals(emptyList(), onNodeWithText("plain line 1200").spanColors())
        tokens = text.lines.map { listOf(CodeToken(0, 5, TokenKind.Keyword)) }
        waitForIdle()
        assertEquals(1200, state.firstVisibleLine)
        assertTrue(keyword in onNodeWithText("plain line 1200").spanColors())
    }

    @Test
    fun suppliedTokensTakeThePlaceOfTheLanguages() = runComposeUiTest {
        var colors: CodeColors? = null
        val code = CodeText.of("val x = 1")
        setStretchContent(StretchThemes.InkDark) {
            colors = codeColors
            CodeViewer(code, Modifier.size(400.dp, 200.dp), language = CodeLanguages.named("kotlin"), tokens = listOf(listOf(CodeToken(4, 5, TokenKind.Type))))
        }
        waitForIdle()
        val spans = onNodeWithText("val x = 1").spanColors()
        assertEquals(listOf(colors!!.type), spans)
    }
}
