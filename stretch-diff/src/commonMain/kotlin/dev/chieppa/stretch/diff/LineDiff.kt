package dev.chieppa.stretch.diff

/** An edit between two sequences: keep the element, remove it from the old one, or add it from the new one. */
internal enum class Edit { Keep, Remove, Add }

/** Above this many differences the search stops and what is left counts as replaced, so huge rewrites stay responsive. */
private const val MAX_EDIT_DISTANCE = 4096

/** Myers' greedy shortest edit script between [old] and [new], with common ends trimmed first. */
internal fun <T> editScript(old: List<T>, new: List<T>, maxDistance: Int = MAX_EDIT_DISTANCE): List<Edit> {
    var prefix = 0
    while (prefix < old.size && prefix < new.size && old[prefix] == new[prefix]) prefix++
    var suffix = 0
    while (suffix < old.size - prefix && suffix < new.size - prefix && old[old.size - 1 - suffix] == new[new.size - 1 - suffix]) suffix++
    val oldMiddle = old.subList(prefix, old.size - suffix)
    val newMiddle = new.subList(prefix, new.size - suffix)
    return List(prefix) { Edit.Keep } + middleScript(oldMiddle, newMiddle, maxDistance) + List(suffix) { Edit.Keep }
}

private fun <T> middleScript(old: List<T>, new: List<T>, maxDistance: Int): List<Edit> {
    if (old.isEmpty()) return List(new.size) { Edit.Add }
    if (new.isEmpty()) return List(old.size) { Edit.Remove }
    val limit = minOf(old.size + new.size, maxDistance)
    val offset = limit + 1
    val furthest = IntArray(2 * limit + 3)
    val trace = ArrayList<IntArray>()
    for (distance in 0..limit) {
        trace += furthest.copyOf()
        for (diagonal in -distance..distance step 2) {
            val down = goesDown(furthest, offset, diagonal, distance)
            var x = if (down) furthest[offset + diagonal + 1] else furthest[offset + diagonal - 1] + 1
            var y = x - diagonal
            while (x < old.size && y < new.size && old[x] == new[y]) {
                x++
                y++
            }
            furthest[offset + diagonal] = x
            if (x >= old.size && y >= new.size) return backtrack(trace, offset, old.size, new.size)
        }
    }
    return List(old.size) { Edit.Remove } + List(new.size) { Edit.Add }
}

private fun goesDown(furthest: IntArray, offset: Int, diagonal: Int, distance: Int) =
    diagonal == -distance || (diagonal != distance && furthest[offset + diagonal - 1] < furthest[offset + diagonal + 1])

private fun backtrack(trace: List<IntArray>, offset: Int, oldSize: Int, newSize: Int): List<Edit> {
    val edits = ArrayList<Edit>()
    var x = oldSize
    var y = newSize
    for (distance in trace.indices.reversed()) {
        val furthest = trace[distance]
        val diagonal = x - y
        val down = goesDown(furthest, offset, diagonal, distance)
        val previousDiagonal = if (down) diagonal + 1 else diagonal - 1
        val previousX = furthest[offset + previousDiagonal]
        val previousY = previousX - previousDiagonal
        while (x > previousX && y > previousY) {
            edits += Edit.Keep
            x--
            y--
        }
        if (distance > 0) {
            edits += if (down) Edit.Add else Edit.Remove
            x = previousX
            y = previousY
        }
    }
    edits.reverse()
    return edits
}

/** Lines of [text], without their endings. A final line ending does not start another line. */
fun splitLines(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    val lines = text.split("\r\n", "\n", "\r")
    return if (text.endsWith("\n") || text.endsWith("\r")) lines.dropLast(1) else lines
}

/** Every line of [old] and [new] in order, unchanged ones included, with word changes marked on replaced lines. */
internal fun diffLines(old: List<String>, new: List<String>): List<DiffLine> {
    val lines = ArrayList<DiffLine>()
    var oldAt = 0
    var newAt = 0
    for (edit in editScript(old, new)) {
        when (edit) {
            Edit.Keep -> lines += DiffLine(LineKind.Context, new[newAt], ++oldAt, ++newAt)
            Edit.Remove -> lines += DiffLine(LineKind.Removed, old[oldAt], oldNumber = ++oldAt)
            Edit.Add -> lines += DiffLine(LineKind.Added, new[newAt], newNumber = ++newAt)
        }
    }
    return markWordChanges(lines)
}

/**
 * The diff of two whole texts, as one hunk holding every line. The viewer folds long unchanged runs, and a person can
 * unfold them. A null path means the file does not exist on that side.
 */
fun diffTexts(old: String, new: String, oldPath: String? = null, newPath: String? = oldPath): FileDiff {
    val lines = diffLines(splitLines(old), splitLines(new))
    val status = when {
        oldPath == null && newPath != null -> FileStatus.Added
        newPath == null && oldPath != null -> FileStatus.Deleted
        oldPath != newPath -> FileStatus.Renamed
        else -> FileStatus.Modified
    }
    return FileDiff(oldPath, newPath, status, if (lines.isEmpty()) emptyList() else listOf(DiffHunk(1, 1, lines)))
}
