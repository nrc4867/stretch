package dev.chieppa.stretch.code

import kotlin.test.Test
import kotlin.test.assertEquals

class LineTokensTest {
    @Test
    fun splitsTokensOntoTheLinesTheyCross() {
        // a0 b1 \r2 \n3 c4 d5 \n6 e7
        val lines = tokensByLine("ab\r\ncd\ne", listOf(CodeToken(1, 5, TokenKind.Comment), CodeToken(7, 8, TokenKind.Number)))
        assertEquals(
            listOf(
                listOf(CodeToken(1, 2, TokenKind.Comment)),
                listOf(CodeToken(0, 1, TokenKind.Comment)),
                listOf(CodeToken(0, 1, TokenKind.Number)),
            ),
            lines,
        )
    }

    @Test
    fun keepsNestedTokensInTheOrderGiven() {
        val lines = tokensByLine("x\nfoo(", listOf(CodeToken(2, 6, TokenKind.Type), CodeToken(2, 5, TokenKind.Function)))
        assertEquals(listOf(CodeToken(0, 4, TokenKind.Type), CodeToken(0, 3, TokenKind.Function)), lines[1])
        assertEquals(emptyList(), lines[0])
    }

    @Test
    fun givesALineForEveryLineOfTheText() {
        assertEquals(CodeText.of("a\n\nb\n").lineCount, tokensByLine("a\n\nb\n", emptyList()).size)
        assertEquals(1, tokensByLine("", emptyList()).size)
    }
}
