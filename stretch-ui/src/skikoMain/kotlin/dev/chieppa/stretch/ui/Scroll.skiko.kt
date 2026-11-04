package dev.chieppa.stretch.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

@Composable
private fun style() = ScrollbarStyle(
    minimalHeight = 24.dp,
    thickness = 6.dp,
    shape = Stretch.shapes.pillShape,
    hoverDurationMillis = 250,
    unhoverColor = Stretch.colors.inkSubtle.copy(alpha = 0.35f),
    hoverColor = Stretch.colors.inkMuted.copy(alpha = 0.7f),
)

@Composable
actual fun Scrollbar(state: ScrollState, modifier: Modifier) {
    VerticalScrollbar(rememberScrollbarAdapter(state), modifier.fillMaxHeight().padding(2.dp), style = style())
}

@Composable
actual fun Scrollbar(state: LazyListState, modifier: Modifier) {
    VerticalScrollbar(rememberScrollbarAdapter(state), modifier.fillMaxHeight().padding(2.dp), style = style())
}
