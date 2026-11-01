package dev.chieppa.stretch.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** A thin themed scrollbar for [state], shown on hover and while scrolling. Align it to the end of the scrolled box. */
@Composable
expect fun Scrollbar(state: ScrollState, modifier: Modifier = Modifier)

/** A thin themed scrollbar for a lazy list. */
@Composable
expect fun Scrollbar(state: LazyListState, modifier: Modifier = Modifier)

/** A column that scrolls vertically with a themed [Scrollbar] over its right edge. */
@Composable
fun ScrollColumn(modifier: Modifier = Modifier, state: ScrollState = rememberScrollState(), content: @Composable ColumnScope.() -> Unit) {
    Box(modifier) {
        Column(Modifier.verticalScroll(state), content = content)
        Scrollbar(state, Modifier.align(Alignment.CenterEnd))
    }
}
