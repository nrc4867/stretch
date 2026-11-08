package dev.chieppa.stretch.diff

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.code.CodeDecorationStyle
import dev.chieppa.stretch.code.CodeRange
import dev.chieppa.stretch.test.annotatedText
import dev.chieppa.stretch.test.characterCenter
import dev.chieppa.stretch.test.characterStart
import dev.chieppa.stretch.test.underlinedRun
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import dev.chieppa.stretch.ui.TOOLTIP_DELAY_MS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DiffSymbolTest {
    private val oldText = (1..60).joinToString("\n") { if (it == 30) "val total = count(items)" else "keep line$it" }
    private val newText = (1..60).joinToString("\n") { if (it == 30) "val total = sum(items)" else "keep line$it" }
    private val file = diffTexts(oldText, newText, "Totals.kt")

    @Test
    fun eachSideAsksAndClicksThroughOnItsOwnLines() = runComposeUiTest {
        val asked = mutableListOf<DiffPosition>()
        val clicks = mutableListOf<DiffSymbolClick>()
        val slots = DiffSlots(symbolAt = { asked += it; wordAt(it) }, onSymbolClick = { clicks += it })
        setContent { StretchTheme(StretchThemes.Ink) { DiffViewer(listOf(file), Modifier.size(900.dp, 500.dp), rememberDiffViewerState(split = true), slots = slots) } }
        val new = onNodeWithText("val total = sum(items)")
        new.performMouseInput { click(new.characterCenter(0)) }
        new.performKeyInput { keyDown(Key.CtrlLeft) }
        new.performMouseInput { moveTo(new.characterCenter(13)) }
        waitForIdle()
        assertEquals(DiffPosition(0, DiffSide.New, 30, 13), asked.last())
        assertEquals(12 to 15, new.underlinedRun())

        val old = onNodeWithText("val total = count(items)")
        old.performMouseInput { moveTo(old.characterCenter(14)) }
        waitForIdle()
        assertEquals(DiffPosition(0, DiffSide.Old, 30, 14), asked.last())
        assertEquals(12 to 17, old.underlinedRun())
        assertNull(new.underlinedRun())

        old.performMouseInput { click(old.characterCenter(14)) }
        waitForIdle()
        val click = clicks.single()
        assertEquals(DiffPosition(0, DiffSide.Old, 30, 14), click.position)
        assertEquals(CodeRange(30, 12, 17), click.range)
        assertTrue(assertNotNull(click.bounds).width > 0f)
    }

    @Test
    fun aKeyClicksThroughFromTheCaretOpeningTheFoldThatHidesIt() = runComposeUiTest {
        val clicks = mutableListOf<DiffSymbolClick>()
        val slots = DiffSlots(symbolAt = { wordAt(it) }, onSymbolClick = { clicks += it })
        val state = DiffViewerState()
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            StretchTheme(StretchThemes.Ink) { DiffViewer(listOf(file), Modifier.size(900.dp, 500.dp), state, slots = slots) }
        }
        val removed = onNodeWithText("val total = count(items)")
        removed.performMouseInput { click(removed.characterStart(12)) }
        waitForIdle()
        assertEquals(DiffPosition(0, DiffSide.Old, 30, 12), state.caret)

        state.caret = DiffPosition(0, DiffSide.New, 5, 6)
        var found = false
        runOnIdle { scope.launch { found = state.clickSymbolAtCaret() } }
        waitForIdle()
        assertTrue(found)
        val click = clicks.single()
        assertEquals(CodeRange(5, 5, 9), click.range)
        assertNotNull(click.bounds)
        onNodeWithText("keep line5").assertExists()
    }

    @Test
    fun anUnchangedLineInOneColumnShowsBothSidesDecorations() = runComposeUiTest {
        val decorations = listOf(
            DiffDecoration(0, DiffSide.Old, CodeRange(29, 0, 4), CodeDecorationStyle.Reference),
            DiffDecoration(0, DiffSide.New, CodeRange(29, 5, 11), CodeDecorationStyle.Error, "Never read"),
            DiffDecoration(0, DiffSide.New, CodeRange(30, 4, 9), CodeDecorationStyle.Definition),
        )
        setContent { StretchTheme(StretchThemes.Ink) { DiffViewer(listOf(file), Modifier.size(900.dp, 500.dp), decorations = decorations) } }
        val unchanged = onNodeWithText("keep line29")
        assertEquals(listOf(0 to 4), unchanged.annotatedText().spanStyles.filter { it.item.background != Color.Unspecified }.map { it.start to it.end })
        val added = onAllNodesWithText("val total = sum(items)")[0]
        assertTrue(added.annotatedText().spanStyles.any { it.start == 4 && it.end == 9 && it.item.background != Color.Unspecified })

        unchanged.performMouseInput { moveTo(unchanged.characterCenter(7)) }
        mainClock.advanceTimeBy(TOOLTIP_WAIT_MS)
        onNodeWithText("Never read").assertExists()
    }

    /** The word of letters at [at] in [file], as an app's lookup might find a name, or null off a word. */
    private fun wordAt(at: DiffPosition): CodeRange? {
        val text = if (at.side == DiffSide.Old) oldText else newText
        val line = text.lines()[at.line - 1]
        if (at.column !in line.indices || !line[at.column].isLetter()) return null
        var start = at.column
        while (start > 0 && line[start - 1].isLetter()) start--
        var end = at.column
        while (end < line.length && line[end].isLetter()) end++
        return CodeRange(at.line, start, end)
    }
}

/** Longer than a tooltip waits before it shows. */
private const val TOOLTIP_WAIT_MS = TOOLTIP_DELAY_MS + 100
