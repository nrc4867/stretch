package dev.chieppa.stretch.frame

/** The ranking [fuzzyMatch] describes. */
internal object FuzzyMatcher {
    private const val PREFIX = 0
    private const val WORD_PREFIX = 1
    private const val CONTAINS = 2

    /** Where scores for a match word by word start; each word adds its own cost. */
    private const val WORDS = 10
    private const val WORD_AT_START = 0
    private const val WORD_INSIDE = 1
    private const val WORD_SLIPPED = 2

    /** Each slipped letter costs this much more. */
    private const val PER_SLIP = 2

    /** Where scores for letters in order start; each letter between them adds one. */
    private const val LETTERS = 1000

    /** Words shorter than this must match exactly; from it one slip is forgiven, from [TWO_SLIPS] two. */
    private const val ONE_SLIP = 4
    private const val TWO_SLIPS = 8

    fun match(query: String, label: String): FuzzyMatch? {
        val q = lower(query.trim())
        if (q.isEmpty()) return FuzzyMatch(PREFIX, emptyList())
        val l = lower(label)
        val starts = wordStarts(label)
        if (l.startsWith(q)) return FuzzyMatch(PREFIX, listOf(q.indices))
        starts.firstOrNull { l.startsWith(q, it) }?.let { return FuzzyMatch(WORD_PREFIX, listOf(it until it + q.length)) }
        l.indexOf(q).takeIf { it >= 0 }?.let { return FuzzyMatch(CONTAINS, listOf(it until it + q.length)) }
        return byWords(q, l, starts) ?: inOrder(q.filterNot(Char::isWhitespace), l)
    }

    /** Lowercases letter by letter, so indices stay those of the label. */
    private fun lower(text: String): String = String(CharArray(text.length) { text[it].lowercaseChar() })

    private fun wordStarts(label: String): List<Int> = label.indices.filter { i ->
        val c = label[i]
        c.isLetterOrDigit() && (i == 0 || !label[i - 1].isLetterOrDigit() || (c.isUpperCase() && label[i - 1].isLowerCase()))
    }

    private fun byWords(q: String, l: String, starts: List<Int>): FuzzyMatch? {
        val words = q.split(' ', '\t').filter { it.isNotEmpty() }
        var score = WORDS
        val ranges = mutableListOf<IntRange>()
        for (word in words) {
            val (cost, range) = word(word, l, starts) ?: return null
            score += cost
            ranges += range
        }
        return FuzzyMatch(score, merged(ranges))
    }

    private fun word(word: String, l: String, starts: List<Int>): Pair<Int, IntRange>? {
        starts.firstOrNull { l.startsWith(word, it) }?.let { return WORD_AT_START to (it until it + word.length) }
        l.indexOf(word).takeIf { it >= 0 }?.let { return WORD_INSIDE to (it until it + word.length) }
        val allowed = when {
            word.length >= TWO_SLIPS -> 2
            word.length >= ONE_SLIP -> 1
            else -> return null
        }
        var best: Pair<Int, IntRange>? = null
        for (start in starts) {
            for (length in word.length - allowed..word.length + allowed) {
                if (length <= 0 || start + length > l.length) continue
                val slips = EditDistance.of(word, l.substring(start, start + length), allowed)
                if (slips <= allowed && (best == null || slips * PER_SLIP + WORD_SLIPPED < best.first)) {
                    best = (WORD_SLIPPED + slips * PER_SLIP) to (start until start + length)
                }
            }
        }
        return best
    }

    /** The tightest run holding [q]'s letters in order: found forwards, then drawn in from its end. */
    private fun inOrder(q: String, l: String): FuzzyMatch? {
        if (q.isEmpty()) return null
        var i = 0
        var end = -1
        for ((index, c) in l.withIndex()) {
            if (c == q[i] && ++i == q.length) {
                end = index
                break
            }
        }
        if (end < 0) return null
        var j = q.lastIndex
        val hits = ArrayList<Int>(q.length)
        for (index in end downTo 0) {
            if (j >= 0 && l[index] == q[j]) {
                hits += index
                j--
            }
        }
        hits.reverse()
        val span = hits.last() - hits.first() + 1
        return FuzzyMatch(LETTERS + span - q.length, merged(hits.map { it..it }))
    }

    private fun merged(ranges: List<IntRange>): List<IntRange> {
        val out = mutableListOf<IntRange>()
        for (range in ranges.sortedBy { it.first }) {
            val last = out.lastOrNull()
            if (last != null && range.first <= last.last + 1) out[out.lastIndex] = last.first..maxOf(last.last, range.last) else out += range
        }
        return out
    }
}
