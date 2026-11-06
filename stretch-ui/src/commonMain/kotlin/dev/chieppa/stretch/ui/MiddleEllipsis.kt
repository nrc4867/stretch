package dev.chieppa.stretch.ui

/** The mark that stands for the characters a shortened text leaves out. */
const val ELLIPSIS = "…"

/**
 * [text] shortened, if it has to be, by taking characters out of its middle until [fits] accepts it: as much of its
 * start and end as fit, joined by [ELLIPSIS]. [suffix] is appended after and never shortened. When even the bare
 * ellipsis and [suffix] do not fit, that is what comes back.
 */
fun middleEllipsis(text: String, suffix: String = "", fits: (String) -> Boolean): String {
    if (fits(text + suffix)) return text + suffix
    var low = 0
    var high = text.length - 1
    while (low < high) {
        val keep = (low + high + 1) / 2
        if (fits(keptEnds(text, keep) + suffix)) low = keep else high = keep - 1
    }
    return keptEnds(text, low) + suffix
}

/** [keep] characters of [text], the first half from its start and the rest from its end, around [ELLIPSIS]. */
private fun keptEnds(text: String, keep: Int): String {
    val end = keep / 2
    return text.take(keep - end) + ELLIPSIS + text.takeLast(end)
}

/**
 * A path's folders and its file name, split at the last `/` or `\`: `"src/ui/App.kt"` gives `"src/ui/"` and
 * `"App.kt"`. A path with no folder gives an empty first part.
 */
fun splitPath(path: String): Pair<String, String> {
    val cut = path.lastIndexOfAny(charArrayOf('/', '\\')) + 1
    return path.take(cut) to path.drop(cut)
}
