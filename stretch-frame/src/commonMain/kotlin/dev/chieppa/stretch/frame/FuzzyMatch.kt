package dev.chieppa.stretch.frame

/** How a query matched a label: its [score], lower is better, and the [ranges] of the label to draw marked. */
data class FuzzyMatch(val score: Int, val ranges: List<IntRange>)

/**
 * Matches [query] against [label] forgivingly, for search fields that suggest as the person types. Best first: the label
 * starts with the query, a word in it does, it holds the query anywhere; then every word of the query found in the label
 * in any order, each at the start of a word, anywhere, or, for a word of letters only, with a slip of one letter (two
 * in long words), so numbers and hashes never match by a slip; then the query's letters in order, closer together
 * ranking higher. A word starts after anything but a letter or digit, and at
 * a capital after a small letter. Case is ignored; a blank query matches everything at 0. Null when nothing matches.
 */
fun fuzzyMatch(query: String, label: String): FuzzyMatch? = FuzzyMatcher.match(query, label)

/** [items] that match [query], best first by [fuzzyMatch] on the best of their [labels]; ties keep their order. */
fun <T> fuzzySearch(query: String, items: List<T>, labels: (T) -> List<String>): List<T> = items
    .mapIndexedNotNull { index, item -> labels(item).mapNotNull { fuzzyMatch(query, it)?.score }.minOrNull()?.let { Triple(it, index, item) } }
    .sortedWith(compareBy({ it.first }, { it.second }))
    .map { it.third }
