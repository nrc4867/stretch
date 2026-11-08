package dev.chieppa.stretch.code

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import dev.chieppa.stretch.ui.TOOLTIP_DELAY_MS
import dev.chieppa.stretch.ui.TooltipAt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * The tooltip of the decorations under the pointer, which [tipAt] finds for a character, shown over their range once
 * the pointer rests there and hidden while Ctrl or Cmd is held. Compose it in the content [CodeSymbolPointer.inner]
 * is the coordinates of.
 */
@Composable
fun <G> CodeDecorationTooltip(pointer: CodeSymbolPointer<G>, tipAt: (CodeSpot<G>) -> DecorationTip?) {
    val currentTipAt by rememberUpdatedState(tipAt)
    var shown by remember { mutableStateOf<Pair<String, Rect>?>(null) }
    LaunchedEffect(pointer) {
        snapshotFlow { pointer.pointer?.takeIf { !pointer.held }?.let { spot -> currentTipAt(spot)?.let { spot.group to it } } }
            .collectLatest { found ->
                shown = null
                if (found == null) return@collectLatest
                delay(TOOLTIP_DELAY_MS)
                val (group, tip) = found
                shown = pointer.boundsOf(group, tip.range, pointer.inner)?.let { tip.text to it }
            }
    }
    shown?.let { (text, bounds) -> TooltipAt(text, bounds) }
}
