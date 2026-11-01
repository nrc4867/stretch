package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

/**
 * A position, scale or rotation as one row of number fields, one per axis, each led by its axis letter ([axes],
 * X Y Z by default; pass two for a 2D point). Every field scrubs, steps and types like a [NumberField];
 * [onValueChange] reports which axis changed. [contentDescription] names the whole value, and each field is
 * announced as it plus its axis, such as "Position X".
 */
@Composable
fun VectorField(
    values: List<Float>,
    onValueChange: (axis: Int, value: Float) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    axes: List<String> = listOf("X", "Y", "Z"),
    unit: String = "",
    step: Float = 1f,
    range: ClosedFloatingPointRange<Float> = -Float.MAX_VALUE..Float.MAX_VALUE,
    fieldWidth: Dp = 76.dp,
    onValueChangeFinished: () -> Unit = {},
) {
    require(values.size == axes.size) { "VectorField has ${values.size} values for ${axes.size} axes" }
    val c = Stretch.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        axes.forEachIndexed { i, axis ->
            NumberField(
                value = values[i],
                onValueChange = { onValueChange(i, it) },
                contentDescription = "$contentDescription $axis",
                unit = unit,
                step = step,
                range = range,
                width = fieldWidth,
                onValueChangeFinished = onValueChangeFinished,
                leading = { Label(axis, Stretch.type.valueSm, c.inkMuted) },
            )
        }
    }
}
