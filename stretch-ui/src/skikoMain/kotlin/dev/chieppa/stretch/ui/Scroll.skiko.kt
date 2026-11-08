package dev.chieppa.stretch.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

/** How thick the bar's thumb is. */
private val thickness = 6.dp

/** Room around the thumb. */
private val inset = 2.dp

@Composable
private fun style() = ScrollbarStyle(
    minimalHeight = 24.dp,
    thickness = thickness,
    shape = Stretch.shapes.pillShape,
    hoverDurationMillis = 250,
    unhoverColor = Stretch.colors.inkSubtle.copy(alpha = 0.35f),
    hoverColor = Stretch.colors.inkMuted.copy(alpha = 0.7f),
)

@Composable
actual fun Scrollbar(state: ScrollState, modifier: Modifier) {
    val bar = modifier.fillMaxHeight().padding(inset)
    if (state.canScrollForward || state.canScrollBackward) VerticalScrollbar(rememberScrollbarAdapter(state), bar, style = style())
    else Spacer(bar.width(thickness))
}

@Composable
actual fun Scrollbar(state: LazyListState, modifier: Modifier) {
    val bar = modifier.fillMaxHeight().padding(inset)
    if (state.canScrollForward || state.canScrollBackward) VerticalScrollbar(rememberScrollbarAdapter(state), bar, style = style())
    else Spacer(bar.width(thickness))
}
