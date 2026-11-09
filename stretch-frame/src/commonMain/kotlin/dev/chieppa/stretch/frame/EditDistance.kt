package dev.chieppa.stretch.frame

/** How many letters must change to turn one word into another, a swap of two neighbours counting as one. */
internal object EditDistance {
    /** The distance from [a] to [b], or more than [limit] as soon as it is sure to be. */
    fun of(a: String, b: String, limit: Int): Int {
        if (kotlin.math.abs(a.length - b.length) > limit) return limit + 1
        var before = IntArray(b.length + 1)
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            var rowBest = current[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var d = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) d = minOf(d, before[j - 2] + 1)
                current[j] = d
                rowBest = minOf(rowBest, d)
            }
            if (rowBest > limit) return limit + 1
            val spare = before
            before = previous
            previous = current
            current = spare
        }
        return previous[b.length]
    }
}
