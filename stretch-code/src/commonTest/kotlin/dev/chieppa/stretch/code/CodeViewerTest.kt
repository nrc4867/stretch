package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CodeViewerTest {
    private val text = CodeText((1..2000).map { if (it % 500 == 0) "val marker$it = $it" else "val line$it = $it" })

    @Test
    fun splitsTextIntoLinesAndFinds() {
        val code = CodeText.of("one\r\ntwo\nthree\rfour\n")
        assertEquals(listOf("one", "two", "three", "four", ""), code.lines)
        assertEquals("two", code.line(2))
        assertEquals(
            listOf(CodeRange(1, 0, 2), CodeRange(1, 3, 5), CodeRange(2, 1, 3)),
            findInLines(listOf("ab ab", "xAB"), "ab"),
        )
        assertEquals(listOf(CodeRange(1, 0, 2), CodeRange(1, 3, 5)), findInLines(listOf("ab ab", "xAB"), "ab", matchCase = true))
        assertEquals(emptyList(), findInLines(listOf("ab"), ""))
    }

    @Test
    fun expandsTabsAndKeepsHighlightsOnTheirCharacters() {
        assertEquals(listOf(0, 4, 5), tabOffsets("\tx").toList())
        val line = codeLineText("\tval", listOf(CodeToken(1, 4, TokenKind.Keyword)), colorsForTest, listOf(CodeHighlight(1, 2, Color.Red)))
        assertEquals("    val", line.text)
        assertEquals(4, line.spanStyles.first { it.item.background == Color.Red }.start)
    }

    @Test
    fun buildsOnlyTheLinesOnScreenAndScrollsToALine() = runComposeUiTest {
        val state = CodeViewerState()
        setContent { StretchTheme(StretchThemes.Ink) { CodeViewer(text, Modifier.size(600.dp, 400.dp), state, CodeLanguages.named("kotlin")) } }
        onNodeWithText("val line1 = 1").assertExists()
        onNodeWithText("val marker1500 = 1500").assertDoesNotExist()
        runOnIdle { runBlocking { state.scrollToLine(1500) } }
        waitForIdle()
        onNodeWithText("val marker1500 = 1500").assertExists()
        assertEquals(1497, state.firstVisibleLine)
    }

    @Test
    fun findsAndMovesBetweenMatches() = runComposeUiTest {
        val state = CodeViewerState()
        setContent { StretchTheme(StretchThemes.Ink) { CodeViewer(text, Modifier.size(600.dp, 400.dp), state) } }
        onNodeWithText("val line1 = 1").performClick()
        onNodeWithText("val line1 = 1").performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.F)
            keyUp(Key.CtrlLeft)
        }
        waitForIdle()
        onNodeWithContentDescription("Find").performTextInput("marker")
        waitForIdle()
        onNodeWithText("1 of 4").assertExists()
        onNodeWithContentDescription("Find").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        onNodeWithText("2 of 4").assertExists()
        onNodeWithText("val marker1000 = 1000").assertExists()
        onNodeWithContentDescription("Find").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        assertEquals(false, state.findOpen)
    }

    @Test
    fun showsMarksTonesAndAnnotations() = runComposeUiTest {
        var clicked = 0
        val slots = CodeViewerSlots(
            gutterMark = { line -> if (line == 2) BasicText("●") },
            lineAnnotation = { line -> if (line == 3) BasicText("A note under line three") },
            onLineNumberClick = { clicked = it },
        )
        setContent {
            StretchTheme(StretchThemes.InkDark) {
                CodeViewer(CodeText.of("first\nsecond\nthird"), Modifier.fillMaxSize(), tones = { if (it == 2) LineTone.Added else null }, slots = slots)
            }
        }
        onAllNodesWithText("●").assertCountEquals(1)
        onNodeWithText("A note under line three").assertExists()
        onNodeWithText("3").performClick()
        assertEquals(3, clicked)
    }
}

private val colorsForTest = CodeColors(Color.Blue, Color.Green, Color.Magenta, Color.Gray, Color.Cyan, Color.Yellow)
