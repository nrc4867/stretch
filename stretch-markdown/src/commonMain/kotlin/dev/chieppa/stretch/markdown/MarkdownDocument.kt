package dev.chieppa.stretch.markdown

/** Where the caret or selection is: offsets into a block's source. */
data class Caret(val block: Long, val start: Int, val end: Int = start)

/** A whole markdown file as blocks, plus where the caret was, so undo puts it back. */
class MarkdownDocument internal constructor(
    val blocks: List<MarkdownBlock>,
    internal val leading: String = "",
    internal val lineEnding: String = "\n",
    val caret: Caret? = null,
) {
    /** The markdown, with everything no one edited exactly as it was read. */
    val text: String by lazy {
        val joined = leading + blocks.joinToString("") { it.source + it.separator }
        if (lineEnding == "\n") joined else joined.replace("\n", lineEnding)
    }

    internal fun with(blocks: List<MarkdownBlock> = this.blocks, caret: Caret? = this.caret) = MarkdownDocument(blocks, leading, lineEnding, caret)

    internal fun indexOf(id: Long) = blocks.indexOfFirst { it.id == id }

    companion object {
        private const val CRLF = "\r\n"

        /** Reads [text]. A file with Windows line endings keeps them. */
        fun parse(text: String): MarkdownDocument {
            val lineEnding = if (CRLF in text) CRLF else "\n"
            val parsed = parseBlocks(text.replace(CRLF, "\n"))
            return MarkdownDocument(parsed.blocks, parsed.leading, lineEnding)
        }
    }
}
