package dev.chieppa.stretch.markdown

/** Wraps the selection in [source] in [marker], or takes the marker off when it is already there. Returns the text and new selection. */
internal fun toggleWrap(source: String, start: Int, end: Int, marker: String): Triple<String, Int, Int> {
    val width = marker.length
    val inside = start >= width && end + width <= source.length && source.substring(start - width, start) == marker && source.substring(end, end + width) == marker
    if (inside) {
        return Triple(source.removeRange(end, end + width).removeRange(start - width, start), start - width, end - width)
    }
    val selected = source.substring(start, end)
    if (selected.length >= 2 * width && selected.startsWith(marker) && selected.endsWith(marker)) {
        return Triple(source.replaceRange(start, end, selected.substring(width, selected.length - width)), start, end - 2 * width)
    }
    return Triple(source.replaceRange(start, end, marker + selected + marker), start + width, end + width)
}

/** Makes a link of the selection, with the caret ready for its address. */
internal fun insertLink(source: String, start: Int, end: Int): Triple<String, Int, Int> {
    val label = source.substring(start, end)
    val text = "[$label]()"
    val caret = start + label.length + 3
    return Triple(source.replaceRange(start, end, text), caret, caret)
}
