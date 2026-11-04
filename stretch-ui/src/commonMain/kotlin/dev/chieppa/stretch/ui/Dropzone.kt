package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons

enum class DropState { Idle, Over, Reject }

/** A file's extension, lowercase and without the dot. */
fun fileExtension(path: String): String = path.substringAfterLast('/').substringAfterLast('\\').substringAfterLast('.', "").lowercase()

/** Whether a drop of [paths] is accepted: at least one file, all with one of [extensions] (any when empty). */
fun acceptsFiles(paths: List<String>, extensions: Set<String>): Boolean =
    paths.isNotEmpty() && (extensions.isEmpty() || paths.all { fileExtension(it) in extensions })

/** Files being dragged over a target. [paths] is null where the platform hides them until the drop. */
internal class HoveredFiles(val paths: List<String>?)

/** Reports files dragged over and dropped on this element from outside the app. [onHover] gets null on exit. */
@Composable
internal expect fun Modifier.fileDropTarget(onHover: (HoveredFiles?) -> Unit, onDrop: (List<String>) -> Boolean): Modifier

/**
 * A target for files dropped from outside the app. Clicking opens the picker through [onBrowse], so dragging is
 * never required. [kinds] lists what is accepted ("PNG, JPG, SVG"); [extensions] decides it.
 */
@Composable
fun Dropzone(
    label: String,
    kinds: String,
    extensions: Set<String>,
    onFiles: (List<String>) -> Unit,
    onBrowse: () -> Unit,
    overLabel: String,
    rejectLabel: String,
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf(DropState.Idle) }
    DropzoneFace(
        state,
        when (state) {
            DropState.Idle -> label
            DropState.Over -> overLabel
            DropState.Reject -> rejectLabel
        },
        kinds,
        label,
        onBrowse,
        modifier.fileDropTarget(
            onHover = { hovered ->
                val paths = hovered?.paths
                state = when {
                    hovered == null -> DropState.Idle
                    paths == null || acceptsFiles(paths, extensions) -> DropState.Over
                    else -> DropState.Reject
                }
            },
            onDrop = { paths ->
                state = DropState.Idle
                acceptsFiles(paths, extensions).also { if (it) onFiles(paths) }
            },
        ),
    )
}

@Composable
internal fun DropzoneFace(state: DropState, title: String, kinds: String, description: String, onBrowse: () -> Unit, modifier: Modifier = Modifier) {
    val c = Stretch.colors
    val radius = Stretch.shapes.card
    val control = rememberControlState()
    val edge = when (state) {
        DropState.Idle -> c.chipEdge
        DropState.Over -> c.ringSelected
        DropState.Reject -> c.warnEdge
    }
    val fill = when (state) {
        DropState.Idle -> control.fill(c.surfacePanel, c.surfaceControl, c.surfaceControlHi)
        DropState.Over -> c.surfaceControl
        DropState.Reject -> c.warnWash
    }
    val ink = if (state == DropState.Reject) c.warnInk else c.ink
    val stroke = Stretch.stroke
    Column(
        modifier
            .heightIn(min = 120.dp)
            .focusRing(control, Stretch.shapes.cardShape)
            .clip(Stretch.shapes.cardShape)
            .background(fill)
            .dashedBorder(edge, stroke, radius, dashed = state == DropState.Idle)
            .clickable(control.source, null, role = Role.Button, onClick = onBrowse)
            .semantics {
                contentDescription = description
                stateDescription = title
            }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Icon(if (state == DropState.Reject) StretchIcons.Warn else StretchIcons.Import, ink, size = 20.dp)
        Label(title, Stretch.type.body.copy(fontWeight = FontWeight.SemiBold), ink)
        Label(kinds, Stretch.type.bodySm, if (state == DropState.Reject) c.warnInk else c.inkMuted)
    }
}

internal fun Modifier.dashedBorder(color: Color, width: Dp, radius: Dp, dashed: Boolean) = drawBehind {
    val w = width.toPx()
    val dash = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())) else null
    drawRoundRect(
        color,
        topLeft = Offset(w / 2, w / 2),
        size = Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx() - w / 2),
        style = Stroke(w, pathEffect = dash),
    )
}
