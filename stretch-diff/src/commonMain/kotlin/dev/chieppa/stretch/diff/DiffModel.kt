package dev.chieppa.stretch.diff

enum class LineKind { Context, Added, Removed }

/** A range of characters in a line, the part that changed against its partner on the other side. */
data class CharRange(val start: Int, val end: Int)

/** One line of a diff. [oldNumber] and [newNumber] count from 1; each is null on the side the line is not on. */
data class DiffLine(
    val kind: LineKind,
    val text: String,
    val oldNumber: Int? = null,
    val newNumber: Int? = null,
    val changes: List<CharRange> = emptyList(),
)

/** A run of nearby changes. The numbers are where it starts on each side, counting from 1. */
data class DiffHunk(val oldStart: Int, val newStart: Int, val lines: List<DiffLine>, val heading: String = "") {
    val header: String
        get() {
            val oldCount = lines.count { it.kind != LineKind.Added }
            val newCount = lines.count { it.kind != LineKind.Removed }
            return "@@ -$oldStart,$oldCount +$newStart,$newCount @@" + if (heading.isEmpty()) "" else " $heading"
        }
}

enum class FileStatus { Modified, Added, Deleted, Renamed, Copied }

/** The changes to one file. A [binary] file has no hunks. */
data class FileDiff(
    val oldPath: String?,
    val newPath: String?,
    val status: FileStatus = FileStatus.Modified,
    val hunks: List<DiffHunk> = emptyList(),
    val binary: Boolean = false,
) {
    /** The path to show: the new one, or the old one for a deleted file. */
    val path: String get() = newPath ?: oldPath.orEmpty()
    val added: Int get() = hunks.sumOf { hunk -> hunk.lines.count { it.kind == LineKind.Added } }
    val removed: Int get() = hunks.sumOf { hunk -> hunk.lines.count { it.kind == LineKind.Removed } }
}
