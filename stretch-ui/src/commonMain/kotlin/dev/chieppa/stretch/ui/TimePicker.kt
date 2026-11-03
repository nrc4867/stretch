package dev.chieppa.stretch.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime

private val timeRow = 28.dp
private const val visibleRows = 5
private const val hoursInDay = 24
private const val minutesInHour = 60
private const val noon = 12

/**
 * An hour list and a minute list, with AM and PM under them when [hour12]. Each list holds focus as one stop, and Up
 * and Down move its choice. Minutes come every [minuteStep], and a time between those steps still shows its own minute.
 */
@Composable
fun TimePicker(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    hourDescription: String,
    minuteDescription: String,
    modifier: Modifier = Modifier,
    minuteStep: Int = 5,
    hour12: Boolean = usesTwelveHourClock(),
) {
    val pm = time.hour >= noon
    val hours = if (hour12) List(noon) { if (it == 0) noon else it } else List(hoursInDay) { it }
    val shownHour = if (hour12) twelveHour(time.hour) else time.hour
    val minutes = (List(minutesInHour / minuteStep) { it * minuteStep } + time.minute).distinct().sorted()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeColumn(hours, shownHour, hourDescription, Modifier.weight(1f)) { hour ->
                onTimeChange(LocalTime(if (hour12) to24Hour(hour, pm) else hour, time.minute))
            }
            TimeColumn(minutes, time.minute, minuteDescription, Modifier.weight(1f)) { minute ->
                onTimeChange(LocalTime(time.hour, minute))
            }
        }
        if (hour12) {
            val (am, pmName) = dayPeriodNames()
            Segmented(
                listOf(false, true),
                pm,
                { onTimeChange(LocalTime(to24Hour(twelveHour(time.hour), it), time.minute)) },
                { if (it) pmName else am },
                Modifier.fillMaxWidth(),
                fill = true,
            )
        }
    }
}

@Composable
private fun TimeColumn(options: List<Int>, selected: Int, description: String, modifier: Modifier, onPick: (Int) -> Unit) {
    val state = rememberControlState()
    val scroll = rememberScrollState()
    val index = options.indexOf(selected)
    val rowPx = with(LocalDensity.current) { timeRow.toPx() }
    LaunchedEffect(index, rowPx) { scroll.scrollTo(((index - visibleRows / 2) * rowPx).toInt().coerceAtLeast(0)) }
    ScrollColumn(
        modifier
            .height(timeRow * visibleRows)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val move = when (event.key) {
                    Key.DirectionUp -> -1
                    Key.DirectionDown -> 1
                    else -> return@onKeyEvent false
                }
                onPick(options[(index + move).mod(options.size)])
                true
            }
            .focusable(interactionSource = state.source)
            .semantics { contentDescription = description },
        scroll,
    ) {
        for (option in options) {
            PickCell(
                text = option.toString().padStart(2, '0'),
                selected = option == selected,
                onClick = { onPick(option) },
                modifier = Modifier.fillMaxWidth().height(timeRow).padding(end = 8.dp, bottom = 1.dp),
                cursor = state.focused && FocusVisibility.keyboard && option == selected,
            )
        }
    }
}
