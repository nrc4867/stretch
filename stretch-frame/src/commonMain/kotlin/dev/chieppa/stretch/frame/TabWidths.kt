package dev.chieppa.stretch.frame

import androidx.compose.ui.unit.dp

/** The narrowest a tab gets, whether its title is short or the strip is crowded. */
internal val TabMinWidth = 72.dp

/** The widest a tab grows before its title is cut short. */
internal val TabMaxWidth = 240.dp

/**
 * The widths tabs that would like [preferred] widths get in a strip [available] wide. When they fit, each keeps its
 * own. When they do not, the widest give way first: every tab wider than some cap shrinks to it, the cap as high as
 * fits, but no tab goes below [min], so a strip too crowded even then scrolls.
 */
internal fun fitTabWidths(preferred: List<Int>, available: Int, min: Int): List<Int> {
    if (preferred.sum() <= available) return preferred
    val sorted = preferred.sortedDescending()
    var rest = sorted.sum()
    var cap = min
    for (k in 1..sorted.size) {
        rest -= sorted[k - 1]
        val share = (available - rest) / k
        if (k == sorted.size || share >= sorted[k]) {
            cap = share
            break
        }
    }
    val limit = cap.coerceAtLeast(min)
    return preferred.map { minOf(it, limit) }
}
