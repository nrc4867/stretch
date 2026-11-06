package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth

/**
 * Tabs side by side, each as wide as its content asks. When together they need more than [available], the widest
 * give way first, down to [TabMinWidth] (see [fitTabWidths]); past that the row is wider than [available], for a
 * scroll around it to take.
 */
@Composable
internal fun TabRow(available: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else Constraints.Infinity
        val preferred = measurables.map { it.maxIntrinsicWidth(height) }
        val widths = fitTabWidths(preferred, available.roundToPx(), TabMinWidth.roundToPx())
        val placeables = measurables.mapIndexed { i, tab ->
            tab.measure(Constraints(minWidth = widths[i], maxWidth = widths[i], maxHeight = constraints.maxHeight))
        }
        layout(constraints.constrainWidth(placeables.sumOf { it.width }), constraints.constrainHeight(placeables.maxOfOrNull { it.height } ?: 0)) {
            var x = 0
            placeables.forEach {
                it.placeRelative(x, 0)
                x += it.width
            }
        }
    }
}
