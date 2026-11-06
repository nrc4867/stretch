package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth

/**
 * A [MenuDivider] then [content], in a menu's column, or nothing at all when [content] puts nothing in the menu, so
 * a section the app leaves empty for some item leaves no stray divider.
 */
@Composable
internal fun MenuSection(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Layout({ MenuDivider(); Column(content = content) }, modifier) { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0)
        val items = measurables[1].measure(loose)
        if (items.height == 0) return@Layout layout(constraints.constrainWidth(0), constraints.constrainHeight(0)) {}
        val divider = measurables[0].measure(loose)
        layout(constraints.constrainWidth(maxOf(divider.width, items.width)), constraints.constrainHeight(divider.height + items.height)) {
            divider.place(0, 0)
            items.place(0, divider.height)
        }
    }
}
