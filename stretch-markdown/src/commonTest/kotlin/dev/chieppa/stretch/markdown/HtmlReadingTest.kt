package dev.chieppa.stretch.markdown

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import dev.chieppa.stretch.markdown.html.HtmlToken
import dev.chieppa.stretch.markdown.html.decodeEntities
import dev.chieppa.stretch.markdown.html.htmlTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HtmlReadingTest {
    /** Bold shows as a weight, so tests can find which text took it. */
    private val styles: RichStyles = { if (it == RichStyle.Bold) SpanStyle(fontWeight = FontWeight.Bold) else SpanStyle() }

    private fun tree(text: String) = viewTree(MarkdownDocument.parse(text).blocks, styles)

    @Test
    fun readsTagsTextAndReferences() {
        val tokens = htmlTokens("""<p class="x">a &amp; b<!-- gone --><br/><img src='p.png' alt="A &quot;pic&quot;"></p>""")
        assertEquals(
            listOf(
                HtmlToken.Open("p", mapOf("class" to "x"), selfClosing = false),
                HtmlToken.Text("a & b"),
                HtmlToken.Open("br", emptyMap(), selfClosing = true),
                HtmlToken.Open("img", mapOf("src" to "p.png", "alt" to "A \"pic\""), selfClosing = true),
                HtmlToken.Close("p"),
            ),
            tokens,
        )
        assertEquals("1 < 2 — '@' ≠ &unknown;", decodeEntities("1 &lt; 2 &mdash; &#39;&#x40;&#39; &#8800; &unknown;"))
    }

    @Test
    fun readsADependabotDescription() {
        val nodes = tree(dependabotDescription)
        val first = assertIs<ViewNode.Block>(nodes[0])
        assertTrue(first.block.source.startsWith("Bumps"))
        val notes = assertIs<ViewNode.Details>(nodes[1])
        assertEquals("Release notes", notes.summary.text.text)
        val sourced = assertIs<ViewNode.Text>(notes.children[0])
        assertEquals("Sourced from actions/checkout's releases.", sourced.text.text.text)
        assertEquals(
            "https://github.com/actions/checkout/releases",
            sourced.text.text.getStringAnnotations(URL_ANNOTATION, 0, sourced.text.text.length).single().item,
        )
        val quote = assertIs<ViewNode.Quote>(notes.children[1])
        assertEquals(2, assertIs<ViewNode.Text>(quote.children[0]).heading)
        val changes = assertIs<ViewNode.Items>(quote.children[1])
        assertEquals(1, changes.items.size)
        val commits = assertIs<ViewNode.Details>(nodes[2])
        assertEquals(2, assertIs<ViewNode.Items>(commits.children.single()).items.size)
        val badge = assertIs<ViewNode.Block>(nodes[3])
        assertEquals("", readOnlyText(badge.block.source, badge.block.kind, styles).text.text)
        assertTrue(nodes.none { it is ViewNode.Block && "automerge" in it.block.source }, "link definitions show nothing")
        assertIs<ViewNode.Block>(nodes[5]).also { assertEquals(BlockKind.Divider, it.block.kind) }
        val commands = assertIs<ViewNode.Details>(nodes[6])
        assertEquals("Dependabot commands and options", commands.summary.text.text)
        assertEquals(3, commands.children.size)
        assertIs<BlockKind.ListItem>(assertIs<ViewNode.Block>(commands.children[1]).block.kind)
        assertEquals(7, nodes.size)
    }

    @Test
    fun readsInlineHtmlInMarkdown() {
        val read = readOnlyText("Line one<br>two &amp; <b>bold</b> <!-- hidden -->H<sub>2</sub>O", BlockKind.Paragraph, styles)
        assertEquals("Line one\ntwo & bold H2O", read.text.text)
        val bold = read.text.spanStyles.single { it.item.fontWeight == FontWeight.Bold }
        assertEquals("bold", read.text.text.substring(bold.start, bold.end))
    }

    @Test
    fun pullsPicturesOutOfText() {
        val linked = readOnlyText("[![Score](https://x.test/b.svg)](https://docs.test)", BlockKind.Paragraph, styles)
        assertEquals("", linked.text.text)
        assertEquals(listOf(Picture("https://x.test/b.svg", "Score", link = "https://docs.test")), linked.pictures)
        val tag = readOnlyText("""See <img src="a.png" alt="A" width="120"> here""", BlockKind.Paragraph, styles)
        assertEquals("See  here", tag.text.text)
        assertEquals(listOf(Picture("a.png", "A", 120)), tag.pictures)
    }

    @Test
    fun aBareAddressIsALink() {
        val read = readOnlyText("https://github.com/owner/repo/pull/12", BlockKind.Paragraph, styles)
        assertEquals("https://github.com/owner/repo/pull/12", read.text.getStringAnnotations(URL_ANNOTATION, 0, read.text.length).single().item)
    }

    @Test
    fun codeKeepsItsTags() {
        val read = readOnlyText("Use `<br>` and `&amp;`", BlockKind.Paragraph, styles)
        assertEquals("Use <br> and &amp;", read.text.text)
    }

    @Test
    fun aParagraphStartingWithATagStaysAParagraph() {
        val nodes = tree("<b>Note:</b> read **this**\n\n<div>\n\nInside\n\n</div>")
        assertIs<ViewNode.Inline>(nodes[0])
        assertEquals("Note: read this", readOnlyText(nodes[0].let { (it as ViewNode.Inline).block.source }, BlockKind.Paragraph, styles).text.text)
        assertIs<ViewNode.Block>(nodes[1])
        assertEquals(2, nodes.size)
    }

    @Test
    fun theEditorKeepsHtmlAsSource() {
        val document = MarkdownDocument.parse(dependabotDescription)
        assertEquals(dependabotDescription, document.text)
        assertTrue(document.blocks.any { it.kind == BlockKind.Raw && it.source.startsWith("<details>") })
    }
}
