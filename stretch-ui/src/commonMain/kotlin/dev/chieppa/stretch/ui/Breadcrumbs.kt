package dev.chieppa.stretch.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons

/**
 * Widths for items sharing [available] room. Items that fit keep their width; when they don't, the longest shrink
 * first, down to a common cap, so a short name never gets cut to spare a long one.
 */
fun fitWidths(widths: List<Int>, available: Int): List<Int> {
    if (widths.sum() <= available) return widths
    var room = available.coerceAtLeast(0)
    var left = widths.size
    var cap = 0
    for (width in widths.sorted()) {
        val share = room / left
        if (width > share) {
            cap = share
            break
        }
        room -= width
        left--
    }
    return widths.map { minOf(it, cap) }
}

/**
 * The path to where a person is: each of [items] but the last is a link back to it, and the last is the current place.
 * In a narrow space the longest names shorten first.
 */
@Composable
fun <T> Breadcrumbs(items: List<T>, label: (T) -> String, onClick: (T) -> Unit, modifier: Modifier = Modifier) {
    val c = Stretch.colors
    Layout(
        content = {
            items.forEachIndexed { i, item ->
                if (i == items.lastIndex) {
                    Label(label(item), Stretch.type.body.copy(fontWeight = FontWeight.SemiBold), c.inkStrong)
                } else {
                    Link(label(item), { onClick(item) })
                    Icon(StretchIcons.ChevronRight, c.inkSubtle)
                }
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        // Items and chevrons alternate, so the even children are the names.
        val chevrons = measurables.filterIndexed { i, _ -> i % 2 == 1 }.map { it.measure(Constraints()) }
        val names = measurables.filterIndexed { i, _ -> i % 2 == 0 }
        val natural = names.map { it.maxIntrinsicWidth(constraints.maxHeight) }
        val room = if (constraints.hasBoundedWidth) constraints.maxWidth - chevrons.sumOf { it.width } else Int.MAX_VALUE
        val fitted = fitWidths(natural, room)
        val placed = names.mapIndexed { i, name -> name.measure(Constraints(maxWidth = fitted[i])) }
        val width = placed.sumOf { it.width } + chevrons.sumOf { it.width }
        val height = (placed + chevrons).maxOfOrNull { it.height } ?: 0
        layout(width.coerceAtLeast(constraints.minWidth), height.coerceAtLeast(constraints.minHeight)) {
            var x = 0
            placed.forEachIndexed { i, name ->
                name.placeRelative(x, (height - name.height) / 2)
                x += name.width
                chevrons.getOrNull(i)?.let {
                    it.placeRelative(x, (height - it.height) / 2)
                    x += it.width
                }
            }
        }
    }
}
