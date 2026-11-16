package dev.chieppa.stretch.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.Popover
import dev.chieppa.stretch.frame.dragSource
import dev.chieppa.stretch.frame.dropTarget
import dev.chieppa.stretch.frame.fuzzySearch
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.ButtonKind
import dev.chieppa.stretch.ui.PickerGrid
import dev.chieppa.stretch.ui.lifted
import dev.chieppa.stretch.ui.TextField
import dev.chieppa.stretch.ui.opensOnPress
import kotlin.math.roundToInt

private val stamps = listOf("Circle", "Square", "Triangle", "Star", "Hexagon", "Arrow", "Cloud", "Spiral", "Leaf", "Moon")

/** A stamp let go on the board, at a point in pixels from the board's top left. */
private data class Placed(val stamp: String, val at: Offset)

/**
 * A searchable [PickerGrid] of stamps whose tiles drag onto a board ([dragSource], [dropTarget]), and a [Popover] too
 * long for its height, with a footer that stays put.
 */
@Composable
internal fun PickAndDropDemo() {
    var query by remember { mutableStateOf("") }
    var cursor by remember { mutableStateOf<String?>(null) }
    val placed = remember { mutableStateListOf<Placed>() }
    var over by remember { mutableStateOf(false) }
    val shown = if (query.isBlank()) stamps else fuzzySearch(query, stamps) { listOf(it) }
    val search = remember { FocusRequester() }
    val grid = remember { FocusRequester() }
    val c = Stretch.colors
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.width(280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                query,
                {
                    query = it
                    cursor = (if (it.isBlank()) stamps else fuzzySearch(it, stamps) { s -> listOf(s) }).firstOrNull()
                },
                Modifier.fillMaxWidth().focusRequester(search).onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> {
                            if (cursor !in shown) cursor = shown.firstOrNull()
                            grid.requestFocus()
                            true
                        }
                        Key.Enter -> cursor?.let { placed += Placed(it, Offset(16f, 16f)) } != null
                        else -> false
                    }
                },
                placeholder = "Search stamps",
                icon = StretchIcons.Search,
            )
            PickerGrid(
                shown, cursor, { cursor = it }, { placed += Placed(it, Offset(16f, 16f)) }, "Stamps",
                Modifier.fillMaxWidth().focusRequester(grid),
                minCell = 80.dp,
                enabled = { it != "Spiral" },
                onLeaveTop = { search.requestFocus() },
            ) { stamp, _ ->
                StampTile(stamp, faint = stamp == "Spiral", modifier = Modifier.dragSource({ stamp.takeIf { it != "Spiral" } }) { StampTile(stamp, lifted = true) })
            }
        }
        Box(
            Modifier
                .size(320.dp, 220.dp)
                .background(if (over) c.surfaceControl else c.surfaceWell, Stretch.shapes.cardShape)
                .border(Stretch.stroke + (if (over) 1.dp else 0.dp), if (over) c.ringSelected else c.linePanel, Stretch.shapes.cardShape)
                .dropTarget<String>(
                    accepts = { true },
                    onHover = { stamp, _ -> over = stamp != null },
                    onDrop = { stamp, at -> placed += Placed(stamp, at); true },
                ),
        ) {
            if (placed.isEmpty()) {
                BasicText("Drop stamps here", Modifier.align(Alignment.Center), style = Stretch.type.body.merge(TextStyle(color = c.inkMuted)))
            }
            for (p in placed) {
                BasicText(p.stamp, Modifier.offset { IntOffset(p.at.x.roundToInt(), p.at.y.roundToInt()) }, style = Stretch.type.body.merge(TextStyle(color = c.inkStrong)))
            }
        }
    }
    LongPopoverDemo()
}

@Composable
private fun StampTile(stamp: String, modifier: Modifier = Modifier, faint: Boolean = false, lifted: Boolean = false) {
    val c = Stretch.colors
    val shape = Stretch.shapes.rowShape
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(if (lifted) Modifier.lifted(shape) else Modifier.border(Stretch.stroke, c.linePanel, shape))
            .padding(6.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        BasicText(stamp, style = Stretch.type.bodySm.merge(TextStyle(color = if (faint) c.inkSubtle else c.ink)))
    }
}

@Composable
private fun LongPopoverDemo() {
    var open by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf(stamps.first()) }
    Box {
        Button("Choose a stamp", { open = true }, Modifier.opensOnPress(open) { open = true })
        Popover(
            open, { open = false }, title = "Stamps", maxHeight = 240.dp,
            footer = { Button("Done", { open = false }, Modifier.fillMaxWidth(), kind = ButtonKind.Primary) },
        ) {
            for (stamp in stamps + stamps.map { "$it outline" }) {
                Button(stamp, { chosen = stamp }, Modifier.fillMaxWidth(), kind = if (stamp == chosen) ButtonKind.Primary else ButtonKind.Quiet)
            }
        }
    }
}
