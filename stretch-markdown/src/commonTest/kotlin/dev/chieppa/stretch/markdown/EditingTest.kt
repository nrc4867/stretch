package dev.chieppa.stretch.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

class EditingTest {
    private fun doc(text: String) = MarkdownDocument.parse(text)

    private fun MarkdownDocument.id(index: Int = 0) = blocks[index].id

    /** The text after pressing Enter at [at] in block [index], and where the caret lands. */
    private fun enter(text: String, at: Int, index: Int = 0): Pair<String, Caret?> {
        val document = doc(text)
        val after = document.splitBlock(document.id(index), at)
        return after.text to after.caret
    }

    @Test
    fun enterSplitsAParagraph() {
        assertEquals("hel\n\nlo", enter("hello", 3).first)
    }

    @Test
    fun enterAfterAHeadingStartsAParagraph() {
        val (text, caret) = enter("# Title", 7)
        assertEquals("# Title\n\n", text)
        assertEquals(0, caret?.start)
        val document = doc("# Title")
        assertEquals(BlockKind.Paragraph, document.splitBlock(document.id(), 7).blocks.last().kind)
    }

    @Test
    fun enterContinuesAList() {
        assertEquals("- a\n- b\n- ", enter("- a\n- b", 3, 1).first)
        assertEquals("- [x] a\n- [ ] ", enter("- [x] a", 7).first)
        val (text, caret) = enter("  - abc", 6)
        assertEquals("  - ab\n  - c", text)
        assertEquals(4, caret?.start)
    }

    @Test
    fun enterRenumbersWhatFollows() {
        assertEquals("1. a\n2. \n3. b\n4. c", enter("1. a\n2. b\n3. c", 4).first)
    }

    @Test
    fun enterOnAnEmptyItemEndsTheList() {
        assertEquals("- a\n\n", enter("- a\n- ", 2, 1).first)
    }

    @Test
    fun enterOnAnEmptyNestedItemMovesItOut() {
        assertEquals("- a\n- ", enter("- a\n  - ", 4, 1).first)
    }

    @Test
    fun enterContinuesAQuoteAndAnEmptyLineEndsIt() {
        assertEquals("> a\n> ", enter("> a", 3).first)
        assertEquals("> a\n\n", enter("> a\n> ", 6).first)
    }

    @Test
    fun backspaceJoinsWithThePreviousBlock() {
        val document = doc("first\n\n## second")
        val after = document.mergeWithPrevious(document.id(1))
        assertEquals("firstsecond", after.text)
        assertEquals(Caret(document.id(0), 5), after.caret)
    }

    @Test
    fun backspaceKeepsADividerAbove() {
        val document = doc("---\n\ntext")
        val after = document.mergeWithPrevious(document.id(1))
        assertEquals("---\n\ntext", after.text)
        assertEquals(document.id(0), after.caret?.block)
        val empty = doc("---\n\nx").let { it.editBlock(it.id(1), "", 0) }
        assertEquals("---", empty.mergeWithPrevious(empty.id(1)).text)
    }

    @Test
    fun strippingAMarkerKeepsTheText() {
        for (source in listOf("## hi", "- hi", "- [ ] hi", "1. hi", "> hi")) {
            val document = doc(source)
            assertEquals("hi", document.stripMarker(document.id()).text, source)
        }
    }

    @Test
    fun aSingleLineBreakCannotJoinBlocksAfterAnEdit() {
        val document = doc("- a\n- b")
        val after = document.stripMarker(document.id())
        assertEquals("a\n\n- b", after.text)
        assertEquals(2, doc(after.text).blocks.size)
    }

    @Test
    fun aPasteBecomesSeveralBlocksWithTheCaretAtItsEnd() {
        val document = doc("start\n\nend")
        val pasted = "one\n\n# two\n\nthree!"
        val after = document.editBlock(document.id(), pasted, pasted.length)
        assertEquals("one\n\n# two\n\nthree!\n\nend", after.text)
        assertEquals(4, after.blocks.size)
        assertEquals(document.id(), after.blocks[0].id)
        assertEquals(after.blocks[2].id, after.caret?.block)
        assertEquals("three!".length, after.caret?.start)
    }

    @Test
    fun typingAFenceClosesIt() {
        val document = doc("```")
        val after = document.editBlock(document.id(), "```", 3)
        assertEquals("```\n```", after.text)
        assertEquals(3, after.caret?.start)
        val typed = after.editBlock(document.id(), "```kotlin\n```", 9)
        assertEquals("kotlin", (typed.blocks[0].kind as BlockKind.Code).language)
    }

    @Test
    fun aBlankLineInCodeStaysInTheBlock() {
        val document = doc("```\nx\n```")
        val after = document.editBlock(document.id(), "```\nx\n\ny\n```", 8)
        assertEquals(1, after.blocks.size)
    }

    @Test
    fun indentingAListItemKeepsItsText() {
        val document = doc("- a\n- b").let { it.with(caret = Caret(it.id(1), 3)) }
        val after = document.indentBlock(document.id(1), 1)
        assertEquals("- a\n  - b", after.text)
        assertEquals(5, after.caret?.start)
        assertEquals("- a\n- b", after.indentBlock(document.id(1), -1).text)
    }

    @Test
    fun indentingANumberedItemUsesItsMarkerWidth() {
        val document = doc("1. a\n2. b")
        assertEquals("1. a\n   2. b", document.indentBlock(document.id(1), 1).text)
    }

    @Test
    fun convertingKeepsTheText() {
        val document = doc("# hi")
        val id = document.id()
        assertEquals("- hi", document.convertBlock(id, BlockType.Bullet).text)
        assertEquals("> hi", document.convertBlock(id, BlockType.Quote).text)
        assertEquals("```\nhi\n```", document.convertBlock(id, BlockType.Code).text)
        assertEquals("hi", document.convertBlock(id, BlockType.Paragraph).text)
        assertEquals("# hi\n\n---", document.convertBlock(id, BlockType.Divider).text)
        val code = doc("```kt\nval x\n```")
        assertEquals("val x", code.convertBlock(code.id(), BlockType.Paragraph).text)
    }

    @Test
    fun aNewDiagramStartsWithSomethingToDraw() {
        val document = doc("")
        val kind = document.convertBlock(document.id(), BlockType.Diagram).blocks[0].kind as BlockKind.Code
        assertEquals("mermaid", kind.language)
    }

    @Test
    fun removingBlocksKeepsTheFileEnding() {
        val document = doc("a\n\nb\n\nc\n")
        assertEquals("a\n\nb\n", document.removeBlocks(2, 2).text)
        assertEquals("b\n\nc\n", document.removeBlocks(0, 0).text)
        assertEquals("", document.removeBlocks(0, 2).text)
        assertEquals("b\n\nc", document.sourceOfRange(1, 2))
    }

    @Test
    fun wrappingTogglesMarkers() {
        assertEquals(Triple("a **b** c", 4, 5), toggleWrap("a b c", 2, 3, "**"))
        assertEquals(Triple("a b c", 2, 3), toggleWrap("a **b** c", 4, 5, "**"))
        assertEquals(Triple("a b c", 2, 3), toggleWrap("a **b** c", 2, 7, "**"))
    }

    @Test
    fun aLinkLeavesTheCaretAtItsAddress() {
        val (text, start, end) = insertLink("see this", 4, 8)
        assertEquals("see [this]()", text)
        assertEquals(11, start)
        assertEquals(11, end)
    }

    @Test
    fun checkingATaskChangesOnlyItsBox() {
        val document = doc("- [ ] see [x] here")
        assertEquals("- [x] see [x] here", document.setChecked(document.id(), true).text)
    }
}
