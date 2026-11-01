package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.core.KeyChord
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Kbd
import kotlinx.coroutines.delay

/** One shown key press, with how many times in a row it was pressed. */
data class KeyCastEntry(val keys: List<String>, val count: Int = 1)

/**
 * Recent key presses for showing on screen, for recordings and teaching. Apps opt in, usually from a setting, by
 * passing one to [StretchApp]. Repeats of the same chord count up instead of piling on.
 */
@Stable
class KeyCastState(private val limit: Int = 4) {
    val entries = mutableStateListOf<KeyCastEntry>()
    internal var serial by mutableIntStateOf(0)

    fun record(chord: KeyChord) {
        val keys = chord.caps()
        val last = entries.lastOrNull()
        if (last?.keys == keys) {
            entries[entries.lastIndex] = last.copy(count = last.count + 1)
        } else {
            entries += KeyCastEntry(keys)
            if (entries.size > limit) entries.removeAt(0)
        }
        serial++
    }

    fun clear() = entries.clear()
}

/** Records every key press that reaches this element into [state], without consuming it. */
fun Modifier.keyCast(state: KeyCastState): Modifier = onPreviewKeyEvent { event ->
    event.chord()?.let(state::record)
    false
}

/** Shows [state]'s recent presses as key caps; they clear a moment after the last press. */
@Composable
fun KeyCast(state: KeyCastState, modifier: Modifier = Modifier) {
    LaunchedEffect(state.serial) {
        delay(1800)
        state.clear()
    }
    if (state.entries.isEmpty()) return
    val c = Stretch.colors
    Row(
        modifier.floating(Stretch.shapes.cardShape, Stretch.shadows.pop).padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (entry in state.entries) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Kbd(entry.keys)
                if (entry.count > 1) BasicText("×${entry.count}", style = Stretch.type.valueSm.merge(TextStyle(color = c.inkMuted)))
            }
        }
    }
}
