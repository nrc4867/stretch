package dev.chieppa.stretch.diff

private val hunkHeader = Regex("""^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@ ?(.*)$""")
private val gitHeader = Regex("""^diff --git "?a/(.+?)"? "?b/(.+?)"?$""")
private const val NO_FILE = "/dev/null"
private const val NO_NEWLINE_MARK = '\\'

/** The files in a unified diff, as `git diff` or `diff -u` print it. Text around the diffs, such as a commit message, is skipped. */
fun parseUnifiedDiff(patch: String): List<FileDiff> {
    val parser = PatchParser()
    patch.split('\n').forEach { parser.line(it.removeSuffix("\r")) }
    return parser.finish()
}

private class PatchParser {
    private val files = ArrayList<FileDiff>()
    private var file: FileDiff? = null
    private val hunks = ArrayList<DiffHunk>()
    private var hunk: OpenHunk? = null

    /** A hunk being read. It ends when the counts in its header are used up, so a removed `--- x` stays a line. */
    private class OpenHunk(val oldStart: Int, val newStart: Int, var oldLeft: Int, var newLeft: Int, val heading: String) {
        val lines = ArrayList<DiffLine>()
        var oldAt = oldStart
        var newAt = newStart
    }

    fun line(text: String) {
        val open = hunk
        if (open != null) {
            if (text.firstOrNull() == NO_NEWLINE_MARK) return
            if (open.oldLeft > 0 || open.newLeft > 0) {
                if (body(text, open)) return
            }
            closeHunk()
        }
        if (text.firstOrNull() == NO_NEWLINE_MARK) return
        header(text)
    }

    /** Reads a line of a hunk's body, or returns false when [text] is not one. An editor may have trimmed a blank context line. */
    private fun body(text: String, hunk: OpenHunk): Boolean {
        when (text.firstOrNull()) {
            ' ', null -> {
                hunk.lines += DiffLine(LineKind.Context, text.drop(1), hunk.oldAt++, hunk.newAt++)
                hunk.oldLeft--
                hunk.newLeft--
            }
            '+' -> {
                hunk.lines += DiffLine(LineKind.Added, text.drop(1), newNumber = hunk.newAt++)
                hunk.newLeft--
            }
            '-' -> {
                hunk.lines += DiffLine(LineKind.Removed, text.drop(1), oldNumber = hunk.oldAt++)
                hunk.oldLeft--
            }
            else -> return false
        }
        return true
    }

    private fun header(text: String) {
        val range = hunkHeader.find(text)
        when {
            range != null -> {
                if (file == null) file = FileDiff(null, null)
                val (oldStart, oldCount, newStart, newCount, heading) = range.destructured
                hunk = OpenHunk(oldStart.toInt(), newStart.toInt(), oldCount.toIntOrNull() ?: 1, newCount.toIntOrNull() ?: 1, heading)
            }
            text.startsWith("diff --git ") -> {
                closeFile()
                val names = gitHeader.find(text)
                file = FileDiff(names?.groupValues?.get(1), names?.groupValues?.get(2))
            }
            text.startsWith("--- ") -> {
                // Plain `diff -u` has no "diff --git" line, so the old path starts the file.
                if (file == null || hunks.isNotEmpty()) {
                    closeFile()
                    file = FileDiff(null, null)
                }
                val path = pathOf(text.removePrefix("--- "))
                file = file?.copy(oldPath = path, status = if (path == null) FileStatus.Added else file!!.status)
            }
            text.startsWith("+++ ") -> {
                val path = pathOf(text.removePrefix("+++ "))
                file = file?.copy(newPath = path, status = if (path == null) FileStatus.Deleted else file!!.status)
            }
            text.startsWith("new file mode") -> file = file?.copy(oldPath = null, status = FileStatus.Added)
            text.startsWith("deleted file mode") -> file = file?.copy(newPath = null, status = FileStatus.Deleted)
            text.startsWith("rename from ") -> file = file?.copy(oldPath = text.removePrefix("rename from "), status = FileStatus.Renamed)
            text.startsWith("rename to ") -> file = file?.copy(newPath = text.removePrefix("rename to "), status = FileStatus.Renamed)
            text.startsWith("copy from ") -> file = file?.copy(oldPath = text.removePrefix("copy from "), status = FileStatus.Copied)
            text.startsWith("copy to ") -> file = file?.copy(newPath = text.removePrefix("copy to "), status = FileStatus.Copied)
            text.startsWith("Binary files ") || text.startsWith("GIT binary patch") -> file = file?.copy(binary = true)
        }
    }

    private fun pathOf(raw: String): String? {
        val path = raw.substringBefore('\t').trim().removeSurrounding("\"")
        return when {
            path == NO_FILE -> null
            path.startsWith("a/") || path.startsWith("b/") -> path.drop(2)
            else -> path
        }
    }

    private fun closeHunk() {
        hunk?.let { hunks += DiffHunk(it.oldStart, it.newStart, markWordChanges(it.lines), it.heading) }
        hunk = null
    }

    private fun closeFile() {
        closeHunk()
        file?.let { files += it.copy(hunks = hunks.toList()) }
        hunks.clear()
        file = null
    }

    fun finish(): List<FileDiff> {
        closeFile()
        return files
    }
}
