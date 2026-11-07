package dev.chieppa.stretch.frame

private val WORD_BREAKS = charArrayOf(' ', '·', '-')

/**
 * Ranks labels against a palette query, best first. A label starting with the query beats a word starting with it,
 * which beats the query anywhere, which beats its letters in order. Labels that match none of these are dropped.
 */
fun <T> searchLabels(query: String, items: List<T>, label: (T) -> String): List<T> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return items
    return items
        .mapIndexedNotNull { index, item -> matchScore(q, label(item))?.let { Triple(it, index, item) } }
        .sortedWith(compareBy({ it.first }, { it.second }))
        .map { it.third }
}

/**
 * How well [label] matches [query], as [searchLabels] ranks: 0 when it starts with the query, 1 when a word does, 2 when
 * it holds the query anywhere, 3 when it holds its letters in order, and null when it does not match. Case is ignored;
 * a blank query matches everything at 0.
 */
fun matchScore(query: String, label: String): Int? {
    val q = query.trim().lowercase()
    val l = label.lowercase()
    return when {
        q.isEmpty() || l.startsWith(q) -> 0
        wordStart(q, l) != null -> 1
        q in l -> 2
        isSubsequence(q, l) -> 3
        else -> null
    }
}

/**
 * The parts of [label] that match [query], for drawing them marked: the run [matchScore] found, or for letters in order
 * each run of them. Empty for a blank query or no match.
 */
fun matchRanges(query: String, label: String): List<IntRange> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    val l = label.lowercase()
    val start = when {
        l.startsWith(q) -> 0
        else -> wordStart(q, l) ?: l.indexOf(q).takeIf { it >= 0 }
    }
    if (start != null) return listOf(start until start + q.length)
    val runs = mutableListOf<IntRange>()
    var i = 0
    for ((index, ch) in l.withIndex()) {
        if (i >= q.length || ch != q[i]) continue
        val last = runs.lastOrNull()
        if (last != null && last.last == index - 1) runs[runs.lastIndex] = last.first..index else runs += index..index
        i++
    }
    return if (i == q.length) runs else emptyList()
}

private fun wordStart(query: String, label: String): Int? =
    label.indices.firstOrNull { i -> (i == 0 || label[i - 1] in WORD_BREAKS) && label.startsWith(query, i) }

private fun isSubsequence(query: String, label: String): Boolean {
    var i = 0
    for (ch in label) if (i < query.length && ch == query[i]) i++
    return i == query.length
}
