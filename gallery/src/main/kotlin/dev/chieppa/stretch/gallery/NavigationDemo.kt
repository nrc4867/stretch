package dev.chieppa.stretch.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.DateInput
import dev.chieppa.stretch.frame.DateTimeInput
import dev.chieppa.stretch.frame.MenuItem
import dev.chieppa.stretch.frame.SplitButton
import dev.chieppa.stretch.frame.TimeInput
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.ButtonKind
import dev.chieppa.stretch.ui.Calendar
import dev.chieppa.stretch.ui.Divider
import dev.chieppa.stretch.ui.Link
import dev.chieppa.stretch.ui.Paginator
import dev.chieppa.stretch.ui.Stepper
import dev.chieppa.stretch.ui.TimePicker
import dev.chieppa.stretch.ui.VerticalDivider
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

private val flow = listOf("Draw", "Fit", "Join", "Rig", "Colour")

@Composable
internal fun DividersAndLinksDemo() {
    Divider()
    Divider(label = "or")
    Row(Modifier.height(24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Link("Open library", {})
        VerticalDivider()
        Link("Read the paper", {}, external = true)
    }
}

@Composable
internal fun SplitButtonDemo() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        SplitButton("Render", {}, "More ways to render", kind = ButtonKind.Primary, icon = StretchIcons.Play) { close ->
            MenuItem("Render as PNG", { close() })
            MenuItem("Render as GIF", { close() })
        }
        SplitButton("Duplicate", {}, "More ways to duplicate") { close ->
            MenuItem("Duplicate mirrored", { close() })
        }
    }
}

@Composable
internal fun SteppersDemo() {
    var step by remember { mutableIntStateOf(2) }
    Stepper(flow, step, Modifier.fillMaxWidth(), onStep = { step = it })
    Stepper(flow, step, vertical = true, problems = mapOf(1 to "The left ear doesn't fit yet"), onStep = { step = it })
}

@Composable
internal fun PaginatorsDemo() {
    var short by remember { mutableIntStateOf(0) }
    var long by remember { mutableIntStateOf(5) }
    Paginator(short, 4, { short = it }, "Previous page", "Next page")
    Paginator(long, 24, { long = it }, "Previous page", "Next page")
}

@Composable
internal fun DatesAndTimesDemo() {
    var date by remember { mutableStateOf<LocalDate?>(LocalDate(2026, 11, 2)) }
    var time by remember { mutableStateOf<LocalTime?>(LocalTime(14, 20)) }
    var moment by remember { mutableStateOf<LocalDateTime?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateInput(date, { date = it }, "Due date", Modifier.width(180.dp), placeholder = "Due date")
            TimeInput(time, { time = it }, "Start time", Modifier.width(140.dp), placeholder = "Start time")
        }
        DateTimeInput(moment, { moment = it }, "Render date", "Render time", Modifier.width(328.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Calendar(date, { date = it }, "Previous month", "Next month")
        TimePicker(time ?: LocalTime(0, 0), { time = it }, "Hour", "Minute", Modifier.width(180.dp))
    }
}
