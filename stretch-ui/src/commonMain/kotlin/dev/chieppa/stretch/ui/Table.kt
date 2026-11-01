package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons

/**
 * One table column. [numeric] right-aligns in mono with tabular figures; put units in the [title] ("Size (px)").
 * A column with a [sortBy] sorts from its header.
 */
class TableColumn<T>(
    val title: String,
    val numeric: Boolean = false,
    val weight: Float = 1f,
    val sortBy: ((T) -> Comparable<*>)? = null,
    val text: (T) -> String,
)

data class TableSort(val column: Int, val ascending: Boolean = true) {
    fun toggled(column: Int) = if (column == this.column) copy(ascending = !ascending) else TableSort(column)
}

/** [rows] ordered by [sort], stable for equal keys. Unsorted when [sort] is null or its column cannot sort. */
fun <T> sortRows(rows: List<T>, columns: List<TableColumn<T>>, sort: TableSort?): List<T> {
    val key = sort?.let { columns.getOrNull(it.column)?.sortBy } ?: return rows
    @Suppress("UNCHECKED_CAST")
    val comparator = compareBy<T> { key(it) as Comparable<Any> }
    return rows.sortedWith(if (sort.ascending) comparator else comparator.reversed())
}

/**
 * A table for anything people compare row by row. Sorting and selection are the caller's state: the header reports
 * [onSortChange], a row click reports [onRowClick].
 */
@Composable
fun <T> Table(
    rows: List<T>,
    columns: List<TableColumn<T>>,
    modifier: Modifier = Modifier,
    sort: TableSort? = null,
    onSortChange: (TableSort) -> Unit = {},
    isSelected: (T) -> Boolean = { false },
    onRowClick: ((T) -> Unit)? = null,
    dense: Boolean = false,
) {
    val c = Stretch.colors
    val numericStyle = Stretch.type.value.copy(fontSize = Stretch.type.bodySm.fontSize, fontFeatureSettings = "tnum")
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(32.dp).background(c.surfacePanel), verticalAlignment = Alignment.CenterVertically) {
            columns.forEachIndexed { index, column ->
                val sorted = sort?.column == index
                val header = Modifier.weight(column.weight).padding(horizontal = 10.dp)
                Row(
                    if (column.sortBy != null) header.clickable(role = Role.Button) { onSortChange(sort?.toggled(index) ?: TableSort(index)) } else header,
                    horizontalArrangement = if (column.numeric) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Label(column.title.uppercase(), Stretch.type.eyebrow, if (sorted) c.inkStrong else c.inkMuted)
                    if (sorted) Icon(if (sort!!.ascending) StretchIcons.ArrowUp else StretchIcons.ArrowDown, c.inkStrong, Modifier.padding(start = 4.dp), size = 12.dp)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
        for (row in sortRows(rows, columns, sort)) {
            val selected = isSelected(row)
            val source = remember { MutableInteractionSource() }
            val hovered by source.collectIsHoveredAsState()
            val pressed by source.collectIsPressedAsState()
            val ink = if (selected) c.onInk else c.ink
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(if (dense) 26.dp else 32.dp)
                    .background(
                        when {
                            selected -> c.onFill
                            pressed -> c.surfaceControlHi
                            hovered -> c.surfaceControl
                            else -> Color.Transparent
                        },
                    )
                    .hoverable(source)
                    .then(if (onRowClick != null) Modifier.clickable(source, null, role = Role.Button) { onRowClick(row) } else Modifier)
                    .semantics { this.selected = selected },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (column in columns) {
                    val style = if (column.numeric) numericStyle.merge(TextStyle(textAlign = TextAlign.End)) else Stretch.type.bodySm
                    Label(column.text(row), style, ink, Modifier.weight(column.weight).padding(horizontal = 10.dp))
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.chartGrid))
        }
    }
}
