package dev.chieppa.stretch.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ParsingTest {
    private val sample = """
        ---
        title: Sample
        ---

        # Title

        A paragraph with **bold**, *italic*, ~~gone~~, `code` and [a link](https://example.com).
        It carries on here.

        Setext heading
        ===

        - one
        - two
          carried on

          second paragraph
            - nested
        - [ ] todo
        - [x] done

        1. first
        2) second

        > a quote
        > on two lines

        > [!WARNING]
        > Careful.

        ```kotlin
        val x = 1

        val y = 2
        ```

        | a | b |
        |---|:-:|
        | 1 | 2 |

        ![alt](pic.png)

        ---

        <div>
        html
        </div>

            indented code

            more

        last line
    """.trimIndent()

    private fun kinds(text: String) = MarkdownDocument.parse(text).blocks.map { it.kind }

    private fun sources(text: String) = MarkdownDocument.parse(text).blocks.map { it.source }

    @Test
    fun everyFileRoundTripsExactly() {
        val files = listOf(
            sample, sample + "\n", sample + "\n\n\n", "\n\n" + sample, sample.replace("\n", "\r\n"), "", "\n", "\n\n", "x", "x\n", "  \n x", "# a\n\n\n\nb",
            "- a\n\n\n- b\n", "```\nnever closed\n\n", "| a |\n|---|", "a\n   \nb", "> q\nlazy", "---", "--- \n---", "***\n", "\t- tabbed\n",
        )
        for (file in files) assertEquals(file, MarkdownDocument.parse(file).text, "round trip of ${file.take(30)}")
    }

    @Test
    fun readsEachBlockKind() {
        val kinds = kinds(sample)
        val expected = listOf(
            BlockKind.Raw::class, BlockKind.Heading::class, BlockKind.Paragraph::class, BlockKind.Heading::class,
            BlockKind.ListItem::class, BlockKind.ListItem::class, BlockKind.ListItem::class, BlockKind.ListItem::class, BlockKind.ListItem::class,
            BlockKind.ListItem::class, BlockKind.ListItem::class,
            BlockKind.Quote::class, BlockKind.Callout::class, BlockKind.Code::class, BlockKind.Table::class, BlockKind.Image::class,
            BlockKind.Divider::class, BlockKind.Raw::class, BlockKind.Raw::class, BlockKind.Paragraph::class,
        )
        assertEquals(expected, kinds.map { it::class })
    }

    @Test
    fun listItemsKeepTheirContinuationLines() {
        assertEquals("- two\n  carried on\n\n  second paragraph", sources("- two\n  carried on\n\n  second paragraph\n    - nested")[0])
    }

    @Test
    fun readsListDetails() {
        val task = assertIs<BlockKind.ListItem>(classifyBlock("  - [x] done"))
        assertEquals(ListStyle.Task, task.style)
        assertEquals(2, task.indent)
        assertTrue(task.checked)
        assertEquals(8, task.markerEnd)
        val ordered = assertIs<BlockKind.ListItem>(classifyBlock("12. x"))
        assertEquals(12, ordered.number)
    }

    @Test
    fun fencesDoNotEndOnShorterFences() {
        assertEquals(1, sources("````\n```\n````").size)
        assertIs<BlockKind.Code>(classifyBlock("~~~py\nx\n~~~"))
    }

    @Test
    fun codeBlocksKeepBlankLinesAndMarkup() {
        val code = sources("```\n# not heading\n\n- not list\n```\n\nafter")
        assertEquals(2, code.size)
    }

    @Test
    fun aDashLineUnderTextIsASetextHeading() {
        assertEquals(BlockKind.Heading(2, setext = true), classifyBlock("Title\n---"))
        assertEquals(BlockKind.Divider, classifyBlock("---"))
    }

    @Test
    fun aListInterruptsAParagraph() {
        assertEquals(2, sources("words\n- item").size)
    }

    @Test
    fun aPipeAloneIsNotATable() {
        assertEquals(BlockKind.Paragraph, classifyBlock("a | b"))
        assertEquals(BlockKind.Table, classifyBlock("a | b\n--|--"))
    }

    @Test
    fun readsInlineMarkup() {
        val text = "plain **bold** *it* ~~no~~ `co*de` [label](http://x) \\* end"
        val markup = parseInline(text)
        fun styled(style: InlineStyle) = markup.spans.first { it.style == style }.let { text.substring(it.start, it.end) }
        assertEquals("bold", styled(InlineStyle.Bold))
        assertEquals("it", styled(InlineStyle.Italic))
        assertEquals("no", styled(InlineStyle.Strike))
        assertEquals("co*de", styled(InlineStyle.Code))
        assertEquals("label", styled(InlineStyle.Link))
        assertEquals("http://x", markup.spans.first { it.style == InlineStyle.Link }.url)
        val visible = text.filterIndexed { index, _ -> markup.markers.none { index in it } }
        assertEquals("plain bold it no co*de label * end", visible)
    }

    @Test
    fun nestedStylesBothApply() {
        val text = "***both***"
        val styles = parseInline(text).spans.map { it.style }.toSet()
        assertEquals(setOf(InlineStyle.Bold, InlineStyle.Italic), styles)
    }

    @Test
    fun blockMarkersCoverPrefixes() {
        fun hiddenText(source: String): String {
            val markup = blockMarkup(source, classifyBlock(source))
            return source.filterIndexed { index, _ -> markup.hidden.none { index in it } }
        }
        assertEquals("Title", hiddenText("## Title"))
        assertEquals("Title", hiddenText("Title\n==="))
        assertEquals("item", hiddenText("- item"))
        assertEquals("todo", hiddenText("  - [ ] todo"))
        assertEquals("one\ntwo", hiddenText("> one\n> two"))
        assertEquals("Careful", hiddenText("> [!NOTE]\n> Careful"))
        assertEquals("x = 1\ny", hiddenText("```py\nx = 1\ny\n```"))
        assertEquals("", hiddenText("```py\n```"))
    }

    @Test
    fun readsAndWritesTables() {
        val table = parseTable("| a | b \\| c |\n|---|:-:|\n| 1 |\n| `x|y` | 3 | extra |")
        assertEquals(listOf("a", "b \\| c"), table.rows[0])
        assertEquals(listOf("1", ""), table.rows[1])
        assertEquals(listOf("`x|y`", "3"), table.rows[2])
        assertEquals(listOf(ColumnAlign.Start, ColumnAlign.Center), table.aligns)
        val round = parseTable(table.toSource())
        assertEquals(table, round)
        assertEquals(BlockKind.Table, classifyBlock(table.toSource()))
    }

    @Test
    fun tableEditsKeepItsShape() {
        val table = parseTable("| a | b |\n|---|---|\n| 1 | 2 |")
        assertEquals(3, table.withRow(1).rows.size)
        assertEquals(3, table.withColumn(0).columns)
        assertEquals(listOf("a", "", "b"), table.withColumn(0).rows[0])
        assertEquals(1, table.withoutColumn(0).columns)
        assertEquals(table, table.withoutRow(0))
        assertEquals("x", table.withCell(1, 1, "x").rows[1][1])
    }
}
