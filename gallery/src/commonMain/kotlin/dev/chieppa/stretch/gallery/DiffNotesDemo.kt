package dev.chieppa.stretch.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.diff.DiffLineRange
import dev.chieppa.stretch.diff.DiffSlots
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Label

/** Slots for the diff demo: drag over line numbers to pick lines, and a note shows under the last of them, marked in the gutter. */
@Composable
internal fun rememberDiffNotes(): DiffSlots {
    var picked by remember { mutableStateOf<DiffLineRange?>(null) }
    val range = picked
    fun holds(path: String, side: Any, number: Int?) = range != null && range.file.path == path && range.side == side && number != null && number in range
    return DiffSlots(
        onLineRangeSelect = { picked = it },
        gutterMark = { ref -> if (holds(ref.file.path, ref.side, ref.number)) Box(Modifier.size(8.dp).background(Stretch.colors.accent, CircleShape)) },
        lineAnnotation = { ref ->
            if (range != null && ref.side == range.side && ref.file.path == range.file.path && ref.number == range.end) {
                Label(
                    "Lines ${range.start} to ${range.end} on the ${range.side.name.lowercase()} side",
                    Stretch.type.body,
                    Stretch.colors.ink,
                    Modifier.fillMaxWidth().padding(8.dp).background(Stretch.colors.surfaceFloat, Stretch.shapes.cardShape).padding(12.dp),
                )
            }
        },
    )
}
