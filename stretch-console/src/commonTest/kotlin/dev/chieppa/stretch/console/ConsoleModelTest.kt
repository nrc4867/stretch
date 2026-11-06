package dev.chieppa.stretch.console

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConsoleModelTest {
    private val esc = 27.toChar()

    @Test
    fun parsesColorsAndStyles() {
        val parser = AnsiParser()
        val runs = parser.parse("plain ${esc}[1;31mbold red${esc}[0m ${esc}[38;5;208morange${esc}[48;2;1;2;3m on rgb${esc}[39;49m back")
        assertEquals(listOf("plain ", "bold red", " ", "orange", " on rgb", " back"), runs.map { it.text })
        assertEquals(AnsiStyle(foreground = AnsiColor.Indexed(1), bold = true), runs[1].style)
        assertEquals(AnsiStyle.Plain, runs[2].style)
        assertEquals(AnsiColor.Indexed(208), runs[3].style.foreground)
        assertEquals(AnsiColor.Rgb(1, 2, 3), runs[4].style.background)
        assertEquals(AnsiStyle.Plain, runs[5].style)
        assertEquals(AnsiColor.Indexed(9), applySgr(AnsiStyle.Plain, "91").foreground)
        assertEquals(AnsiStyle.Plain, applySgr(AnsiStyle(bold = true), ""))
    }

    @Test
    fun dropsOtherSequencesAndWaitsForCutOnes() {
        val parser = AnsiParser()
        assertEquals("ab", parser.parse("a${esc}[2Kb${esc}]0;title${7.toChar()}").joinToString("") { it.text })
        val first = parser.parse("x${esc}[3")
        assertEquals(listOf("x"), first.map { it.text })
        val second = parser.parse("2my")
        assertEquals(listOf("y"), second.map { it.text })
        assertEquals(AnsiColor.Indexed(2), second[0].style.foreground)
    }

    @Test
    fun linesGrowBreakAndRewriteOnCarriageReturn() {
        val state = ConsoleState()
        state.append("Building")
        assertEquals(listOf("Building"), state.lines.map { it.text })
        state.append("...\nprogress 10%")
        state.append("\rprogress 90%")
        state.append("\r\ndone\n\n")
        assertEquals(listOf("Building...", "progress 90%", "done", ""), state.lines.map { it.text })
        state.append("oops\n", ConsoleStream.Err)
        assertEquals(ConsoleStream.Err, state.lines.last().spans.single().stream)
        state.clear()
        assertTrue(state.lines.isEmpty())
    }

    @Test
    fun keepsOnlyTheNewestLinesWithSteadyIds() {
        val state = ConsoleState(maxLines = 3)
        repeat(5) { state.append("line $it\n") }
        assertEquals(listOf("line 2", "line 3", "line 4"), state.lines.map { it.text })
        assertEquals(listOf(2L, 3L, 4L), state.lines.map { it.id })
    }

    @Test
    fun findsPathsWithLinesAndColumns() {
        val found = PathLineLinks.find("e: src/main/App.kt:12:5 Unresolved; see C:\\work\\Tool.java:40 and Makefile.am:3, not 12:30 or localhost:8080")
        assertEquals(listOf("src/main/App.kt", "C:\\work\\Tool.java", "Makefile.am"), found.map { it.target })
        assertEquals(12, found[0].line)
        assertEquals(5, found[0].column)
        assertNull(found[1].column)
        val text = "e: src/main/App.kt:12:5 Unresolved"
        val link = PathLineLinks.find(text).single()
        assertEquals("src/main/App.kt:12:5", text.substring(link.start, link.end))
    }

    @Test
    fun findsTextAcrossLines() {
        val state = ConsoleState()
        state.append("Warning: one\nfine\nwarning: two warning\n")
        assertEquals(
            listOf(ConsoleMatch(0, 0, 7), ConsoleMatch(2, 0, 7), ConsoleMatch(2, 13, 20)),
            findInConsole(state.lines, "warning"),
        )
        assertEquals(2, findInConsole(state.lines, "warning", matchCase = true).size)
    }
}
