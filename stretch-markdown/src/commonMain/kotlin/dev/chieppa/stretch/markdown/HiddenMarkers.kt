package dev.chieppa.stretch.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Where a caret goes when markers are hidden, so moving it never takes a key press with nothing to see. A caret on a
 * hidden run sits just after it; moving left onto one carries on past the visible character before it.
 */
internal fun snapCaret(old: Int, new: Int, hidden: List<IntRange>, length: Int): Int {
    val run = hidden.firstOrNull { new in it.first..it.last } ?: return new
    val after = (run.last + 1).coerceAtMost(length)
    val movingLeft = new < old
    return if (movingLeft && run.first > 0 && old == after) (run.first - 1).let { before -> if (hidden.any { before in it }) new else before } else after
}

internal fun snapSelection(old: TextRange, new: TextRange, hidden: List<IntRange>, length: Int): TextRange =
    if (new.collapsed) TextRange(snapCaret(old.start, new.start, hidden, length)) else new

/**
 * A delete that took a hidden marker's character out instead takes the visible character it meant. When that empties a
 * span, its markers go too, so no stray `****` turns up.
 */
internal fun deleteThroughMarkers(old: TextFieldValue, new: TextFieldValue, markup: BlockMarkup): TextFieldValue {
    if (new.text.length != old.text.length - 1 || !old.selection.collapsed) return new
    val deleted = new.selection.start
    val run = markup.hidden.firstOrNull { deleted in it } ?: return new
    if (run.first == 0) return new
    val target = run.first - 1
    if (markup.hidden.any { target in it }) return new
    val span = markup.spans.firstOrNull { it.start == target && it.end == target + 1 }
    if (span != null) {
        val opening = markup.markers.firstOrNull { it.last + 1 == span.start } ?: return new
        val closing = markup.markers.firstOrNull { it.first == span.end } ?: return new
        return TextFieldValue(old.text.removeRange(opening.first, closing.last + 1), TextRange(opening.first))
    }
    return TextFieldValue(old.text.removeRange(target, target + 1), TextRange(target))
}
