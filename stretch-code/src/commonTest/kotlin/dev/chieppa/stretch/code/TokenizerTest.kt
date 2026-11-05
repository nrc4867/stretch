package dev.chieppa.stretch.code

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TokenizerTest {
    private fun kinds(text: String, language: String): List<Pair<String, TokenKind>> =
        tokenize(text, CodeLanguages.named(language)).map { text.substring(it.start, it.end) to it.kind }

    @Test
    fun readsKotlin() {
        assertEquals(
            listOf(
                "val" to TokenKind.Keyword,
                "\"hi\"" to TokenKind.String,
                "// done" to TokenKind.Comment,
            ),
            kinds("val x = \"hi\" // done", "kotlin"),
        )
    }

    @Test
    fun findsFunctionsTypesAndNumbers() {
        assertEquals(
            listOf("Foo" to TokenKind.Type, "bar" to TokenKind.Function, "42" to TokenKind.Number, "true" to TokenKind.Number),
            kinds("Foo bar(42, true)", "java"),
        )
    }

    @Test
    fun anEscapedQuoteDoesNotEndAString() {
        assertEquals(listOf("\"a\\\"b\"" to TokenKind.String), kinds("\"a\\\"b\"", "go"))
    }

    @Test
    fun anUnclosedStringEndsWithItsLine() {
        val tokens = kinds("\"open\nreturn", "go")
        assertEquals("\"open" to TokenKind.String, tokens[0])
        assertEquals("return" to TokenKind.Keyword, tokens[1])
    }

    @Test
    fun aBlockCommentCarriesAcrossLines() {
        val lines = listOf("a /* one", "two", "three */ return")
        val tokens = tokenizeLines(lines, CodeLanguages.named("java"))
        assertEquals(listOf(CodeToken(2, 8, TokenKind.Comment)), tokens[0])
        assertEquals(listOf(CodeToken(0, 3, TokenKind.Comment)), tokens[1])
        assertEquals(TokenKind.Comment, tokens[2][0].kind)
        assertEquals(TokenKind.Keyword, tokens[2][1].kind)
    }

    @Test
    fun anApostropheInAWordOpensNoString() {
        assertTrue(kinds("don't return", "python").any { it == "return" to TokenKind.Keyword })
    }

    @Test
    fun sqlIgnoresCase() {
        assertEquals("SELECT" to TokenKind.Keyword, kinds("SELECT 1", "sql")[0])
    }

    @Test
    fun markupColorsTagsNotProse() {
        val tokens = kinds("<a href=\"x\">don't if</a>", "html")
        assertEquals("a" to TokenKind.Keyword, tokens[0])
        assertTrue("\"x\"" to TokenKind.String in tokens)
        assertTrue(tokens.none { it.first == "if" })
    }

    @Test
    fun dataMarksKeys() {
        assertEquals("name" to TokenKind.Function, kinds("name: x", "yaml")[0])
    }

    @Test
    fun findsLanguages() {
        assertEquals("kotlin", CodeLanguages.forPath("src/Main.KT")?.id)
        assertEquals("typescript", CodeLanguages.named("JS")?.id)
        assertNull(CodeLanguages.forPath("README"))
        assertNull(CodeLanguages.named("nonsense"))
        assertNotNull(CodeLanguages.forPath("C:\\work\\a.py"))
    }

    @Test
    fun unknownLanguageHasNoTokens() {
        assertEquals(emptyList(), tokenize("return", null))
    }

    @Test
    fun tokensNeverOverlapOrLeaveTheText() {
        val sample = "fun a(b: Int) = \"\"\"x\ny\"\"\" /* c */ + 1.5e3 // end\n<b class='z'>t</b> \$x 'q\n"
        for (language in CodeLanguages.all) {
            var last = 0
            for (token in tokenize(sample, language)) {
                assertTrue(token.start >= last && token.end > token.start && token.end <= sample.length, "${language.id}: $token")
                last = token.end
            }
        }
    }
}
