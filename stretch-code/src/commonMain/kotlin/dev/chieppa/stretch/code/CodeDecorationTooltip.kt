package dev.chieppa.stretch.code

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import dev.chieppa.stretch.ui.TOOLTIP_DELAY_MS
import dev.chieppa.stretch.ui.TooltipAt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * The tooltip of the decorations under the pointer, from those its line draws ([CodeLineGeometry.decorations]), shown
 * over their range once the pointer rests there and hidden while Ctrl or Cmd is held. Compose it in the content
 * [CodeSymbolPointer.inner] is the coordinates of.
 */
@Composable
fun <G> CodeDecorationTooltip(pointer: CodeSymbolPointer<G>) {
    var shown by remember { mutableStateOf<Pair<String, Rect>?>(null) }
    LaunchedEffect(pointer) {
        snapshotFlow {
            pointer.pointer?.takeIf { !pointer.held }?.let { spot ->
                pointer.lineOf(spot)?.let { line -> decorationTip(line.decorations, spot.column)?.let { line to it } }
            }
        }.collectLatest { found ->
            shown = null
            if (found == null) return@collectLatest
            delay(TOOLTIP_DELAY_MS)
            val (line, tip) = found
            shown = pointer.inner?.let { line.bounds(tip.range.start, tip.range.end, it) }?.let { tip.text to it }
        }
    }
    shown?.let { (text, bounds) -> TooltipAt(text, bounds) }
}
