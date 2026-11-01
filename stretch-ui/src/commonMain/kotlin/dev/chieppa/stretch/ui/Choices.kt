package dev.chieppa.stretch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

/**
 * A checkbox for picking several things from a list ("Export: Body, Head, Tail"). For a single on or off setting use
 * [Toggle]. [checked] null is the mixed state of a parent whose children differ; clicking it checks all.
 */
@Composable
fun Checkbox(checked: Boolean?, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = RoundedCornerShape(4.dp)
    val on = checked != false
    val markInk = listOf(c.inkStrong, c.surfaceFloat).maxBy { contrast(it, c.toggleOn) }
    Row(
        modifier
            .focusRing(state, Stretch.shapes.rowShape)
            .heightIn(min = 28.dp)
            .padding(horizontal = 8.dp)
            .hoverable(state.source)
            .triStateToggleable(
                state = when (checked) {
                    true -> ToggleableState.On
                    false -> ToggleableState.Off
                    null -> ToggleableState.Indeterminate
                },
                interactionSource = state.source,
                indication = null,
                role = Role.Checkbox,
                onClick = { onCheckedChange(checked != true) },
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(16.dp)
                .clip(shape)
                .background(if (on) c.toggleOn else if (state.hovered) c.surfaceControlHi else c.surfaceFloat)
                .border(Stretch.stroke, if (on) c.toggleOnEdge else c.toggleOffEdge, shape),
        ) {
            if (checked != false) {
                Canvas(Modifier.size(16.dp)) {
                    val ink = markInk
                    val w = 1.75.dp.toPx()
                    if (checked == true) {
                        val path = Path().apply {
                            moveTo(size.width * 0.25f, size.height * 0.52f)
                            lineTo(size.width * 0.43f, size.height * 0.70f)
                            lineTo(size.width * 0.76f, size.height * 0.32f)
                        }
                        drawPath(path, ink, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    } else {
                        drawLine(ink, Offset(size.width * 0.28f, size.height / 2), Offset(size.width * 0.72f, size.height / 2), w, StrokeCap.Round)
                    }
                }
            }
        }
        Label(label, Stretch.type.body, c.ink)
    }
}

/** One option of a [RadioGroup]. */
@Composable
fun Radio(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    val c = Stretch.colors
    val state = rememberControlState()
    Row(
        modifier
            .focusRing(state, Stretch.shapes.rowShape)
            .heightIn(min = 28.dp)
            .padding(horizontal = 8.dp)
            .hoverable(state.source)
            .selectable(selected, state.source, null, role = Role.RadioButton, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (state.hovered && !selected) c.surfaceControlHi else c.surfaceFloat)
                .border(Stretch.stroke, if (selected) c.toggleOnEdge else c.toggleOffEdge, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(8.dp).clip(CircleShape).background(c.toggleOnEdge))
        }
        Label(label, Stretch.type.body, c.ink)
    }
}

/**
 * Two to five exclusive choices that need a few words each ("Apply to: this part / all parts"). For short labels
 * that fit in one row use [Segmented].
 */
@Composable
fun <T> RadioGroup(options: List<T>, selected: T, onSelect: (T) -> Unit, label: (T) -> String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 2.dp)) {
        for (option in options) Radio(option == selected, { onSelect(option) }, label(option))
    }
}
