package dev.chieppa.stretch.code

/** Runs of lines a view draws as blocks, looked up by line. */
object CodeBlocks {
    /**
     * Each line of [blocks] mapped to the run it is in, among [lineCount] lines: empty runs, runs outside the text and
     * runs overlapping an earlier one are left out.
     */
    fun byLine(blocks: List<IntRange>, lineCount: Int): Map<Int, IntRange> {
        val at = HashMap<Int, IntRange>()
        for (block in blocks.sortedBy { it.first }) {
            if (block.isEmpty() || block.first < 1 || block.last > lineCount || block.any { it in at }) continue
            for (line in block) at[line] = block
        }
        return at
    }
}
