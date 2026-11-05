package dev.chieppa.stretch.diff

/** Longer lines than this are not compared word by word. */
private const val MAX_WORD_DIFF_LENGTH = 2000

/** Under this share of unchanged characters a pair of lines is a rewrite: marking its words would mark everything. */
private const val MIN_SHARED_RATIO = 0.3

private val wordPattern = Regex("""[\p{L}\p{N}_]+|\s+|.""")

private fun wordsOf(text: String): List<String> = wordPattern.findAll(text).map { it.value }.toList()

/** The changed parts of two versions of a line, or nothing for both when they share too little to be worth showing. */
internal fun wordChanges(old: String, new: String): Pair<List<CharRange>, List<CharRange>> {
    val none = emptyList<CharRange>() to emptyList<CharRange>()
    if (old.length > MAX_WORD_DIFF_LENGTH || new.length > MAX_WORD_DIFF_LENGTH) return none
    val oldWords = wordsOf(old)
    val newWords = wordsOf(new)
    val oldRanges = ArrayList<CharRange>()
    val newRanges = ArrayList<CharRange>()
    var oldAt = 0
    var newAt = 0
    var oldWord = 0
    var newWord = 0
    var shared = 0
    for (edit in editScript(oldWords, newWords)) {
        when (edit) {
            Edit.Keep -> {
                shared += oldWords[oldWord].length
                oldAt += oldWords[oldWord++].length
                newAt += newWords[newWord++].length
            }
            Edit.Remove -> {
                val end = oldAt + oldWords[oldWord++].length
                oldRanges.addMerged(oldAt, end)
                oldAt = end
            }
            Edit.Add -> {
                val end = newAt + newWords[newWord++].length
                newRanges.addMerged(newAt, end)
                newAt = end
            }
        }
    }
    val longest = maxOf(old.length, new.length)
    if (longest == 0 || shared.toDouble() / longest < MIN_SHARED_RATIO) return none
    return oldRanges to newRanges
}

private fun MutableList<CharRange>.addMerged(start: Int, end: Int) {
    val last = lastOrNull()
    if (last != null && last.end == start) this[lastIndex] = CharRange(last.start, end) else add(CharRange(start, end))
}

/**
 * Marks changed words on [lines]. Within a run of removals followed by additions, the nth removed line pairs with the
 * nth added one.
 */
internal fun markWordChanges(lines: List<DiffLine>): List<DiffLine> {
    val marked = lines.toMutableList()
    var at = 0
    while (at < marked.size) {
        if (marked[at].kind != LineKind.Removed) {
            at++
            continue
        }
        val removedStart = at
        while (at < marked.size && marked[at].kind == LineKind.Removed) at++
        val addedStart = at
        while (at < marked.size && marked[at].kind == LineKind.Added) at++
        for (pair in 0 until minOf(addedStart - removedStart, at - addedStart)) {
            val old = marked[removedStart + pair]
            val new = marked[addedStart + pair]
            val (oldChanges, newChanges) = wordChanges(old.text, new.text)
            marked[removedStart + pair] = old.copy(changes = oldChanges)
            marked[addedStart + pair] = new.copy(changes = newChanges)
        }
    }
    return marked
}
