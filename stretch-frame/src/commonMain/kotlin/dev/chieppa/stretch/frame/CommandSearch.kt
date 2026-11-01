package dev.chieppa.stretch.frame

/**
 * Ranks labels against a palette query, best first. A label starting with the query beats a word starting with it,
 * which beats the query anywhere, which beats its letters in order. Labels that match none of these are dropped.
 */
fun <T> searchLabels(query: String, items: List<T>, label: (T) -> String): List<T> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return items
    return items
        .mapIndexedNotNull { index, item -> score(q, label(item).lowercase())?.let { Triple(it, index, item) } }
        .sortedWith(compareBy({ it.first }, { it.second }))
        .map { it.third }
}

private fun score(query: String, label: String): Int? = when {
    label.startsWith(query) -> 0
    label.split(' ', '·', '-').any { it.startsWith(query) } -> 1
    query in label -> 2
    isSubsequence(query, label) -> 3
    else -> null
}

private fun isSubsequence(query: String, label: String): Boolean {
    var i = 0
    for (ch in label) if (i < query.length && ch == query[i]) i++
    return i == query.length
}
