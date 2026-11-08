package dev.chieppa.stretch.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * A thin themed scrollbar for [state], shown on hover and while scrolling, with no thumb while there is nothing to
 * scroll. It takes all the height it is given, so in
 * a box that wraps its content, such as a popup's, give it `Modifier.matchParentSize().wrapContentWidth(Alignment.End)`
 * to keep it from stretching the box; in a box of fixed height, align it to the end.
 */
@Composable
expect fun Scrollbar(state: ScrollState, modifier: Modifier = Modifier)

/** A thin themed scrollbar for a lazy list, with no thumb while the whole list shows. */
@Composable
expect fun Scrollbar(state: LazyListState, modifier: Modifier = Modifier)

/** A column that scrolls vertically with a themed [Scrollbar] over its right edge. */
@Composable
fun ScrollColumn(modifier: Modifier = Modifier, state: ScrollState = rememberScrollState(), content: @Composable ColumnScope.() -> Unit) {
    Box(modifier) {
        Column(Modifier.verticalScroll(state), content = content)
        Scrollbar(state, Modifier.matchParentSize().wrapContentWidth(Alignment.End))
    }
}

/**
 * Scrolls the least distance that shows item [index] whole: up to it when it is above the view, down until it is the
 * last one showing when it is below. Does nothing when it already shows.
 */
suspend fun LazyListState.reveal(index: Int) {
    val visible = layoutInfo.visibleItemsInfo
    if (visible.isEmpty()) {
        scrollToItem(index)
        return
    }
    val first = visible.first()
    val last = visible.last()
    val end = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
    when {
        index < first.index || (index == first.index && first.offset < layoutInfo.viewportStartOffset) -> scrollToItem(index)
        index == last.index && last.offset + last.size > end -> scrollBy((last.offset + last.size - end).toFloat())
        index > last.index -> {
            scrollToItem(index)
            // The item now sits at the top, or lower when the list ran out; bring its bottom up to the view's end.
            val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
            val gap = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding - (item.offset + item.size)
            if (gap > 0) scrollBy(-gap.toFloat())
        }
    }
}
