package dev.chieppa.stretch.diff

/** What a row of a diff is to a [DiffBlock]: its first row, which draws it, or a later row, which takes no room. */
internal sealed interface DiffBlockRole {
    data class Start(val block: DiffBlock) : DiffBlockRole

    data object Inside : DiffBlockRole
}

/** Finds which rows of a diff draw blocks: each block whose lines all show as unchanged rows, one after another. */
internal object DiffBlockRows {
    fun of(rows: List<DiffRow>, blocks: List<DiffBlock>): Map<String, DiffBlockRole> {
        if (blocks.isEmpty()) return emptyMap()
        val byFile = blocks.groupBy { it.fileIndex }
        val roles = HashMap<String, DiffBlockRole>()
        var i = 0
        while (i < rows.size) {
            val start = unchangedOf(rows[i])
            val block = start?.let { (file, line) -> byFile[file]?.firstOrNull { it.lines.first == line } }
            val size = block?.let { it.lines.last - it.lines.first + 1 } ?: 0
            val whole = block != null && size > 0 &&
                (0 until size).all { offset -> rows.getOrNull(i + offset)?.let(::unchangedOf) == block.fileIndex to block.lines.first + offset }
            if (block != null && whole) {
                roles[rows[i].key] = DiffBlockRole.Start(block)
                for (offset in 1 until size) roles[rows[i + offset].key] = DiffBlockRole.Inside
                i += size
            } else {
                i++
            }
        }
        return roles
    }

    /** The file and new-side number of an unchanged line's row, or null for any other row. */
    private fun unchangedOf(row: DiffRow): Pair<Int, Int>? = when (row) {
        is DiffRow.Unified -> row.line.takeIf { it.kind == LineKind.Context }?.newNumber?.let { row.fileIndex to it }
        is DiffRow.Split -> {
            val right = row.right
            val bothUnchanged = right != null && right.kind == LineKind.Context && row.left?.kind == LineKind.Context
            if (bothUnchanged) right?.newNumber?.let { row.fileIndex to it } else null
        }
        else -> null
    }
}
