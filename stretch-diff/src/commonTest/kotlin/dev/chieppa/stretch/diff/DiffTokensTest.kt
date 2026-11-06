package dev.chieppa.stretch.diff

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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.code.CodeColors
import dev.chieppa.stretch.code.CodeToken
import dev.chieppa.stretch.code.TokenKind
import dev.chieppa.stretch.code.codeColors
import dev.chieppa.stretch.code.tokensByLine
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchThemes
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
class DiffTokensTest {
    private val oldText = (1..40).joinToString("\n") { "word $it" }
    private val newText = (1..40).joinToString("\n") { if (it == 20) "other 20" else "word $it" }
    private val file = diffTexts(oldText, newText, "notes.kt")
    private val untouched = diffTexts("fun a() = 1", "fun a() = 2", "other.kt")

    /** Whole-file tokens marking the first word of every line as [kind]. */
    private fun firstWords(text: String, kind: TokenKind) =
        tokensByLine(text, Regex("(?m)^\\w+").findAll(text).map { CodeToken(it.range.first, it.range.last + 1, kind) }.toList())

    @Test
    fun usesWholeFileTokensForEachSideAndKeepsTheRest() = runComposeUiTest {
        var tokens by mutableStateOf(emptyMap<FileDiff, FileTokens>())
        var colors: CodeColors? = null
        val state = DiffViewerState(split = true)
        setStretchContent(StretchThemes.Ink) {
            colors = codeColors
            DiffViewer(listOf(file, untouched), Modifier.size(900.dp, 800.dp), state, tokens = tokens)
        }
        waitForIdle()
        assertEquals(emptyList(), onNodeWithText("other 20").spanColors().filter { it == colors!!.type })
        val before = state.list.firstVisibleItemIndex
        tokens = mapOf(file to FileTokens(old = firstWords(oldText, TokenKind.String), new = firstWords(newText, TokenKind.Type)))
        waitForIdle()
        assertEquals(before, state.list.firstVisibleItemIndex)
        assertTrue(colors!!.type in onNodeWithText("other 20").spanColors())
        assertTrue(colors!!.string in onNodeWithText("word 20").spanColors())
        // The other file keeps the built-in tokenizer's keyword.
        assertTrue(colors!!.keyword in onAllNodesWithText("fun a() = 2")[0].spanColors())
    }
}
