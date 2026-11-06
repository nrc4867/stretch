package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CodeViewerWrapTest {
    private val long = "word ".repeat(60).trimEnd()
    private val code = CodeText(listOf("short", long, "after"))

    @Test
    fun wrapsLongLinesWithTheNumberMarkAndNoteInPlace() = runComposeUiTest {
        var clicked = 0
        val slots = CodeViewerSlots(
            gutterMark = { line -> if (line == 2) BasicText("●") },
            lineAnnotation = { line -> if (line == 2) BasicText("note two") },
            onLineNumberClick = { clicked = it },
        )
        setStretchContent(StretchThemes.Ink) {
            CodeViewer(code, Modifier.size(320.dp, 500.dp), rememberCodeViewerState(wrap = true), tones = { if (it == 2) LineTone.Added else null }, slots = slots)
        }
        val short = onNodeWithText("short").getUnclippedBoundsInRoot()
        val wrapped = onNodeWithText(long).getUnclippedBoundsInRoot()
        assertTrue(wrapped.height > short.height * 3, "the long line wraps onto several rows")
        assertTrue(wrapped.right <= 320.dp, "the wrapped line stays inside the viewer")
        assertEquals(wrapped.top, onNodeWithText("2").getUnclippedBoundsInRoot().top, "the number sits on the first row")
        assertTrue(onNodeWithText("●").getUnclippedBoundsInRoot().top < wrapped.top + short.height, "the gutter mark sits on the first row")
        val note = onNodeWithText("note two").getUnclippedBoundsInRoot()
        assertTrue(note.top >= wrapped.bottom, "the note sits under the whole line")
        assertTrue(onNodeWithText("after").getUnclippedBoundsInRoot().top >= note.bottom)
        onNodeWithText("2").performClick()
        assertEquals(2, clicked)
    }

    @Test
    fun keepsLongLinesOnOneRowWithoutWrap() = runComposeUiTest {
        setStretchContent(StretchThemes.Ink) { CodeViewer(code, Modifier.size(320.dp, 500.dp)) }
        assertEquals(onNodeWithText("short").getUnclippedBoundsInRoot().height, onNodeWithText(long).getUnclippedBoundsInRoot().height)
    }
}
