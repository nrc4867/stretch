package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons

private val stepNode = 24.dp
private val stepConnectorMin = 16.dp

/**
 * Where a person is in a run of steps: numbered marks joined by a line, with finished steps checked and [current]
 * ringed. A step in [problems] shows the warning mark, and its message in the mark's tooltip. With [onStep] each step
 * is a button; without it the stepper only reports. Laid out across its width, or down with [vertical].
 */
@Composable
fun Stepper(
    steps: List<String>,
    current: Int,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    problems: Map<Int, String> = emptyMap(),
    onStep: ((Int) -> Unit)? = null,
) {
    val c = Stretch.colors
    if (vertical) {
        Column(modifier) {
            steps.forEachIndexed { i, step ->
                StepItem(step, i, current, problems[i], onStep)
                if (i < steps.lastIndex) {
                    Box(
                        Modifier
                            .padding(start = (stepNode - Stretch.stroke) / 2)
                            .width(Stretch.stroke)
                            .height(stepConnectorMin)
                            .background(if (i < current) c.lineStrong else c.chipEdge),
                    )
                }
            }
        }
    } else {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            steps.forEachIndexed { i, step ->
                StepItem(step, i, current, problems[i], onStep)
                if (i < steps.lastIndex) {
                    Box(Modifier.weight(1f).widthIn(min = stepConnectorMin).height(Stretch.stroke).background(if (i < current) c.lineStrong else c.chipEdge))
                }
            }
        }
    }
}

@Composable
private fun StepItem(label: String, index: Int, current: Int, problem: String?, onStep: ((Int) -> Unit)?) {
    val c = Stretch.colors
    val state = rememberControlState()
    val isCurrent = index == current
    val press = if (onStep == null) Modifier else Modifier.hoverable(state.source).clickable(state.source, null, role = Role.Button) { onStep(index) }
    Row(
        Modifier
            .focusRing(state, Stretch.shapes.segShape)
            .clip(Stretch.shapes.segShape)
            .then(press)
            .semantics(mergeDescendants = true) {
                selected = isCurrent
                if (problem != null) error(problem)
            }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (problem != null) Tooltip(problem) { StepMark(index, current, problem = true) } else StepMark(index, current, problem = false)
        val ink = when {
            problem != null -> c.warnInk
            isCurrent || state.hovered -> c.inkStrong
            index < current -> c.ink
            else -> c.inkMuted
        }
        Label(label, Stretch.type.body.copy(fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal), ink)
    }
}

@Composable
private fun StepMark(index: Int, current: Int, problem: Boolean) {
    val c = Stretch.colors
    val shape = Stretch.shapes.pillShape
    val done = index < current
    val (fill, edge, ink) = when {
        problem -> Triple(c.warnWash, c.warnEdge, c.warnInk)
        done -> Triple(c.onFill, c.onEdge, c.onInk)
        index == current -> Triple(c.surfaceFloat, c.ringSelected, c.inkStrong)
        else -> Triple(Color.Transparent, c.chipEdge, c.inkMuted)
    }
    Box(
        Modifier.size(stepNode).clip(shape).background(fill).border(if (index == current) Stretch.stroke + focusGrowth else Stretch.stroke, edge, shape),
        contentAlignment = Alignment.Center,
    ) {
        when {
            problem -> Icon(StretchIcons.Warn, ink, size = 14.dp)
            done -> Icon(StretchIcons.Check, ink, size = 14.dp)
            else -> Label((index + 1).toString(), Stretch.type.valueSm, ink)
        }
    }
}
