package dev.chieppa.stretch.markdown

/** The block types people can pick from the toolbar or the slash menu. */
enum class BlockType { Paragraph, Heading1, Heading2, Heading3, Bullet, Numbered, Task, Quote, Callout, Code, Diagram, Table, Image, Divider }

/** New source for a block of [type] holding [text]. */
internal fun sourceOf(type: BlockType, text: String): String = when (type) {
    BlockType.Paragraph -> text
    BlockType.Heading1 -> "# $text"
    BlockType.Heading2 -> "## $text"
    BlockType.Heading3 -> "### $text"
    BlockType.Bullet -> "- $text"
    BlockType.Numbered -> "1. $text"
    BlockType.Task -> "- [ ] $text"
    BlockType.Quote -> text.split('\n').joinToString("\n") { "> $it" }
    BlockType.Callout -> "> [!NOTE]\n" + text.split('\n').joinToString("\n") { "> $it" }
    BlockType.Code -> "```\n$text\n```"
    BlockType.Diagram -> "```mermaid\n${text.ifEmpty { NEW_DIAGRAM }}\n```"
    BlockType.Table -> MarkdownTable(listOf(listOf(text, ""), listOf("", "")), listOf(ColumnAlign.Start, ColumnAlign.Start)).toSource()
    BlockType.Image -> "![$text]()"
    BlockType.Divider -> "---"
}

private const val NEW_DIAGRAM = "flowchart LR\n    A --> B"
