package dev.chieppa.stretch.diff

/** One row the viewer draws. */
sealed interface DiffRow {
    /** A stable key within the viewer's list. */
    val key: String

    data class FileHeader(override val key: String, val file: FileDiff, val fileIndex: Int, val collapsed: Boolean) : DiffRow

    data class HunkHeader(override val key: String, val fileIndex: Int, val hunkIndex: Int, val hunk: DiffHunk) : DiffRow

    /** Unchanged lines folded away, [lineCount] of them. */
    data class Fold(override val key: String, val id: FoldId, val lineCount: Int) : DiffRow

    /** One line in the unified layout. */
    data class Unified(override val key: String, val fileIndex: Int, val hunkIndex: Int, val line: DiffLine) : DiffRow

    /** A pair of lines side by side; either side can be blank. */
    data class Split(override val key: String, val fileIndex: Int, val hunkIndex: Int, val left: DiffLine?, val right: DiffLine?) : DiffRow

    /** A change the file could not show as lines, such as a binary file. */
    data class Binary(override val key: String) : DiffRow
}

/** Which folded run a [DiffRow.Fold] is: its first line within the hunk. */
data class FoldId(val fileIndex: Int, val hunkIndex: Int, val firstLine: Int)

/** Unchanged lines kept around a change when the rest of a run folds away. */
const val FOLD_CONTEXT_LINES = 3

/** Runs that would hide fewer lines than this stay open: a fold row takes up as much room as they do. */
private const val MIN_FOLDED_LINES = 2

/** The rows of [files]. Runs of unchanged lines fold, except those in [unfolded], and files in [collapsed] show only their header. */
fun diffRows(
    files: List<FileDiff>,
    split: Boolean,
    unfolded: Set<FoldId> = emptySet(),
    collapsed: Set<Int> = emptySet(),
    context: Int = FOLD_CONTEXT_LINES,
): List<DiffRow> = buildList {
    files.forEachIndexed { fileIndex, file ->
        val isCollapsed = fileIndex in collapsed
        add(DiffRow.FileHeader("f$fileIndex", file, fileIndex, isCollapsed))
        if (isCollapsed) return@forEachIndexed
        if (file.binary) add(DiffRow.Binary("b$fileIndex"))
        file.hunks.forEachIndexed { hunkIndex, hunk ->
            val wholeFile = hunk.oldStart == 1 && hunk.newStart == 1 && hunk.heading.isEmpty() && file.hunks.size == 1
            if (!wholeFile) add(DiffRow.HunkHeader("h$fileIndex.$hunkIndex", fileIndex, hunkIndex, hunk))
            addHunk(fileIndex, hunkIndex, hunk, split, unfolded, context)
        }
    }
}

private fun MutableList<DiffRow>.addHunk(fileIndex: Int, hunkIndex: Int, hunk: DiffHunk, split: Boolean, unfolded: Set<FoldId>, context: Int) {
    val lines = hunk.lines
    var at = 0
    while (at < lines.size) {
        if (lines[at].kind != LineKind.Context) {
            at = addChanges(fileIndex, hunkIndex, lines, at, split)
            continue
        }
        var end = at
        while (end < lines.size && lines[end].kind == LineKind.Context) end++
        val keepBefore = if (at == 0) 0 else context
        val keepAfter = if (end == lines.size) 0 else context
        val hidden = end - at - keepBefore - keepAfter
        val id = FoldId(fileIndex, hunkIndex, at + keepBefore)
        if (hidden < MIN_FOLDED_LINES || id in unfolded) {
            addContext(fileIndex, hunkIndex, lines, at, end, split)
        } else {
            addContext(fileIndex, hunkIndex, lines, at, at + keepBefore, split)
            add(DiffRow.Fold("x$fileIndex.$hunkIndex.${id.firstLine}", id, hidden))
            addContext(fileIndex, hunkIndex, lines, end - keepAfter, end, split)
        }
        at = end
    }
}

private fun MutableList<DiffRow>.addContext(fileIndex: Int, hunkIndex: Int, lines: List<DiffLine>, from: Int, to: Int, split: Boolean) {
    for (index in from until to) {
        val line = lines[index]
        val key = "l$fileIndex.$hunkIndex.$index"
        add(if (split) DiffRow.Split(key, fileIndex, hunkIndex, line, line) else DiffRow.Unified(key, fileIndex, hunkIndex, line))
    }
}

/** Adds the run of changes starting at [from] and returns where it ends. Side by side, removals pair with the additions after them. */
private fun MutableList<DiffRow>.addChanges(fileIndex: Int, hunkIndex: Int, lines: List<DiffLine>, from: Int, split: Boolean): Int {
    var end = from
    while (end < lines.size && lines[end].kind != LineKind.Context) end++
    if (!split) {
        for (index in from until end) add(DiffRow.Unified("l$fileIndex.$hunkIndex.$index", fileIndex, hunkIndex, lines[index]))
        return end
    }
    val removed = (from until end).filter { lines[it].kind == LineKind.Removed }
    val added = (from until end).filter { lines[it].kind == LineKind.Added }
    for (pair in 0 until maxOf(removed.size, added.size)) {
        val left = removed.getOrNull(pair)
        val right = added.getOrNull(pair)
        add(DiffRow.Split("l$fileIndex.$hunkIndex.${left ?: right}", fileIndex, hunkIndex, left?.let(lines::get), right?.let(lines::get)))
    }
    return end
}

/** Row indices where a run of changes starts, for moving between changes. */
fun changeStarts(rows: List<DiffRow>): List<Int> {
    fun changed(row: DiffRow?) = when (row) {
        is DiffRow.Unified -> row.line.kind != LineKind.Context
        is DiffRow.Split -> row.left?.kind != LineKind.Context || row.right?.kind != LineKind.Context
        else -> false
    }
    return rows.indices.filter { changed(rows[it]) && !changed(rows.getOrNull(it - 1)) }
}
