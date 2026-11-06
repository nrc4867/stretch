package dev.chieppa.stretch.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.Label
import dev.chieppa.stretch.ui.Segmented
import dev.chieppa.stretch.ui.Toggle
import kotlinx.coroutines.launch

/** The bar over a [DiffViewer]: the totals, moving between changes, wrapping and the layout. */
@Composable
internal fun DiffBar(files: List<FileDiff>, state: DiffViewerState, strings: DiffStrings) {
    val c = Stretch.colors
    val scope = rememberCoroutineScope()
    val added = remember(files) { files.sumOf { it.added } }
    val removed = remember(files) { files.sumOf { it.removed } }
    Row(
        Modifier.fillMaxWidth().background(c.surfaceRail).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(strings.summary(files.size, added, removed), Stretch.type.bodySm, c.inkMuted, Modifier.weight(1f))
        IconButton(StretchIcons.ArrowUp, strings.previousChange, { scope.launch { state.previousChange() } })
        IconButton(StretchIcons.ArrowDown, strings.nextChange, { scope.launch { state.nextChange() } })
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Label(strings.wrap, Stretch.type.bodySm, c.inkMuted)
            Toggle(state.wrap, { state.wrap = it }, strings.wrap)
        }
        Segmented(listOf(false, true), state.split, { state.split = it }, { if (it) strings.split else strings.unified })
    }
    Box(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
}
