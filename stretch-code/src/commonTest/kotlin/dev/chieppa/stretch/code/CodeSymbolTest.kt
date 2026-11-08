package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CodeSymbolTest {
    private val text = CodeText.of("val greeting = hello(name)\nprintln(greeting)")

    @Test
    fun holdingCtrlUnderlinesTheSymbolAndAClickGoesThroughToIt() = runComposeUiTest {
        val asked = mutableListOf<CodePosition>()
        val clicks = mutableListOf<CodeSymbolClick>()
        val slots = CodeViewerSlots(symbolAt = { asked += it; wordAt(text, it) }, onSymbolClick = { clicks += it })
        setContent { StretchTheme(StretchThemes.Ink) { CodeViewer(text, Modifier.size(600.dp, 300.dp), slots = slots) } }
        val line = onNodeWithText("val greeting = hello(name)")
        line.performMouseInput { click(line.characterCenter(16)) }
        assertEquals(emptyList(), clicks)
        assertNull(line.underlined())

        line.performKeyInput { keyDown(Key.CtrlLeft) }
        line.performMouseInput { moveTo(line.characterCenter(16)) }
        waitForIdle()
        assertEquals(15 to 20, line.underlined())
        assertEquals(CodePosition(1, 16), asked.last())
        val before = asked.size
        line.performMouseInput { moveTo(line.characterCenter(18)) }
        waitForIdle()
        assertEquals(before, asked.size, "moving within the underlined symbol asks nothing")

        line.performMouseInput { click(line.characterCenter(18)) }
        waitForIdle()
        val click = clicks.single()
        assertEquals(CodePosition(1, 18), click.position)
        assertEquals(CodeRange(1, 15, 20), click.range)
        val bounds = assertNotNull(click.bounds)
        assertTrue(bounds.width > 0f && bounds.height > 0f)

        line.performKeyInput { keyUp(Key.CtrlLeft) }
        waitForIdle()
        assertNull(line.underlined())
    }

    @Test
    fun aSlowAnswerLeavesTheLastSymbolUnderlinedUntilItComes() = runComposeUiTest {
        val gate = CompletableDeferred<Unit>()
        val slots = CodeViewerSlots(
            symbolAt = { if (it.line == 2) gate.await(); wordAt(text, it) },
            onSymbolClick = {},
        )
        setContent { StretchTheme(StretchThemes.Ink) { CodeViewer(text, Modifier.size(600.dp, 300.dp), slots = slots) } }
        val first = onNodeWithText("val greeting = hello(name)")
        val second = onNodeWithText("println(greeting)")
        first.performMouseInput { click(first.characterCenter(0)) }
        first.performKeyInput { keyDown(Key.CtrlLeft) }
        first.performMouseInput { moveTo(first.characterCenter(5)) }
        waitForIdle()
        assertEquals(4 to 12, first.underlined())

        second.performMouseInput { moveTo(second.characterCenter(10)) }
        waitForIdle()
        assertEquals(4 to 12, first.underlined())
        assertNull(second.underlined())

        gate.complete(Unit)
        waitForIdle()
        assertNull(first.underlined())
        assertEquals(8 to 16, second.underlined())
    }

    @Test
    fun aPlainPressPutsTheCaretAndAKeyClicksThroughFromIt() = runComposeUiTest {
        val long = CodeText((1..2000).map { if (it == 1500) "call(target)" else "line $it" })
        val clicks = mutableListOf<CodeSymbolClick>()
        val slots = CodeViewerSlots(symbolAt = { wordAt(long, it) }, onSymbolClick = { clicks += it })
        val state = CodeViewerState()
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            StretchTheme(StretchThemes.Ink) { CodeViewer(long, Modifier.size(600.dp, 300.dp), state, slots = slots) }
        }
        val line = onNodeWithText("line 3")
        line.performMouseInput { click(line.characterStart(2)) }
        waitForIdle()
        assertEquals(CodePosition(3, 2), state.caret)

        state.caret = CodePosition(1500, 6)
        var found = false
        runOnIdle { scope.launch { found = state.clickSymbolAtCaret() } }
        waitForIdle()
        assertTrue(found)
        val click = clicks.single()
        assertEquals(CodeRange(1500, 5, 11), click.range)
        assertNotNull(click.bounds)
        onNodeWithText("call(target)").assertExists()
    }

    @Test
    fun withoutSymbolsACtrlPressIsLeftToTheText() = runComposeUiTest {
        setContent { StretchTheme(StretchThemes.Ink) { CodeViewer(text, Modifier.size(600.dp, 300.dp)) } }
        val line = onNodeWithText("val greeting = hello(name)")
        line.performMouseInput { click(line.characterCenter(1)) }
        line.performKeyInput { keyDown(Key.CtrlLeft) }
        line.performMouseInput { moveTo(line.characterCenter(16)) }
        waitForIdle()
        assertNull(line.underlined())
    }
}

/** The word of letters at [at] in [text], as an app's lookup might find a name, or null off a word. */
internal fun wordAt(text: CodeText, at: CodePosition): CodeRange? {
    val line = text.line(at.line)
    if (at.column !in line.indices || !line[at.column].isLetter()) return null
    var start = at.column
    while (start > 0 && line[start - 1].isLetter()) start--
    var end = at.column
    while (end < line.length && line[end].isLetter()) end++
    return CodeRange(at.line, start, end)
}
