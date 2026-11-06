package dev.chieppa.stretch.code

import androidx.compose.foundation.ScrollState

/** Characters kept showing before a column scrolled to sideways. */
const val REVEAL_MARGIN_CHARS = 8

/**
 * Scrolls a code view sideways, when [column] (counted after tabs expand) is out of view, so it shows with [margin]
 * characters before it. [charWidth] is one character's width in pixels; nothing moves while it is unknown.
 */
suspend fun ScrollState.revealColumn(column: Int, charWidth: Float, margin: Int = REVEAL_MARGIN_CHARS) {
    if (charWidth <= 0f) return
    val x = (column * charWidth).toInt()
    if (x < value || x > value + viewportSize - charWidth * margin) scrollTo((x - (charWidth * margin).toInt()).coerceAtLeast(0))
}
