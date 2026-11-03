package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.date_invalid
import dev.chieppa.stretch.frame.generated.date_open
import dev.chieppa.stretch.frame.generated.month_next
import dev.chieppa.stretch.frame.generated.month_previous
import dev.chieppa.stretch.frame.generated.time_hour
import dev.chieppa.stretch.frame.generated.time_invalid
import dev.chieppa.stretch.frame.generated.time_minute
import dev.chieppa.stretch.frame.generated.time_open
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.Calendar
import dev.chieppa.stretch.ui.CalendarWidth
import dev.chieppa.stretch.ui.FieldIcon
import dev.chieppa.stretch.ui.TextField
import dev.chieppa.stretch.ui.TimePicker
import dev.chieppa.stretch.ui.formatDate
import dev.chieppa.stretch.ui.formatTime
import dev.chieppa.stretch.ui.parseDate
import dev.chieppa.stretch.ui.parseTime
import dev.chieppa.stretch.ui.usesTwelveHourClock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource

private val popoverPadding = 24.dp
private val timePopoverWidth = 200.dp

/**
 * A date to type or pick from a [Calendar] in a popover. Typing commits on Enter or when focus leaves, Esc takes it
 * back, and an empty field means no date. Text that isn't a date, or a date outside [range], shows the field's error.
 */
@Composable
fun DateInput(
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    range: ClosedRange<LocalDate>? = null,
) {
    PickerField(
        value, onValueChange, ::formatDate, { text -> parseDate(text)?.takeIf { range == null || it in range } },
        contentDescription, modifier, placeholder, StretchIcons.Calendar, stringResource(Res.string.date_open), stringResource(Res.string.date_invalid),
        CalendarWidth + popoverPadding,
    ) { close ->
        Calendar(
            value,
            {
                onValueChange(it)
                close()
            },
            stringResource(Res.string.month_previous),
            stringResource(Res.string.month_next),
            range = range,
        )
    }
}

/**
 * A time to type or pick from a [TimePicker] in a popover, which stays open since each change applies at once. Typing
 * works as in [DateInput]. The picker starts from [value], or midnight with no value.
 */
@Composable
fun TimeInput(
    value: LocalTime?,
    onValueChange: (LocalTime?) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minuteStep: Int = 5,
    hour12: Boolean = usesTwelveHourClock(),
) {
    PickerField(
        value, onValueChange, { formatTime(it, hour12) }, ::parseTime,
        contentDescription, modifier, placeholder, StretchIcons.Clock, stringResource(Res.string.time_open), stringResource(Res.string.time_invalid),
        timePopoverWidth,
    ) { _ ->
        TimePicker(
            value ?: LocalTime(0, 0),
            onValueChange,
            stringResource(Res.string.time_hour),
            stringResource(Res.string.time_minute),
            minuteStep = minuteStep,
            hour12 = hour12,
        )
    }
}

/**
 * A [DateInput] and a [TimeInput] side by side for one moment. [onValueChange] gets null until both parts are filled;
 * the part already entered stays in its field meanwhile.
 */
@Composable
fun DateTimeInput(
    value: LocalDateTime?,
    onValueChange: (LocalDateTime?) -> Unit,
    dateDescription: String,
    timeDescription: String,
    modifier: Modifier = Modifier,
    range: ClosedRange<LocalDate>? = null,
    minuteStep: Int = 5,
    hour12: Boolean = usesTwelveHourClock(),
) {
    var date by remember { mutableStateOf(value?.date) }
    var time by remember { mutableStateOf(value?.time) }
    LaunchedEffect(value) {
        if (value != null) {
            date = value.date
            time = value.time
        }
    }
    fun push() {
        val d = date
        val t = time
        onValueChange(if (d != null && t != null) LocalDateTime(d, t) else null)
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DateInput(date, { date = it; push() }, dateDescription, Modifier.weight(1.3f), range = range)
        TimeInput(time, { time = it; push() }, timeDescription, Modifier.weight(1f), minuteStep = minuteStep, hour12 = hour12)
    }
}

@Composable
private fun <T : Any> PickerField(
    value: T?,
    onValueChange: (T?) -> Unit,
    format: (T) -> String,
    parse: (String) -> T?,
    contentDescription: String,
    modifier: Modifier,
    placeholder: String,
    icon: ImageVector,
    openDescription: String,
    invalidMessage: String,
    popoverWidth: Dp,
    picker: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val shown = value?.let(format).orEmpty()
    var text by remember(shown) { mutableStateOf(shown) }
    var invalid by remember(shown) { mutableStateOf(false) }
    var open by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }

    fun commit() {
        val typed = text.trim()
        if (typed == shown) {
            invalid = false
            return
        }
        if (typed.isEmpty()) {
            invalid = false
            onValueChange(null)
            return
        }
        val parsed = parse(typed)
        if (parsed == null) {
            invalid = true
        } else {
            invalid = false
            text = format(parsed)
            onValueChange(parsed)
        }
    }

    Box(modifier) {
        TextField(
            text,
            {
                text = it
                invalid = false
            },
            Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (hadFocus && !it.isFocused) commit()
                    hadFocus = it.isFocused
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Enter -> {
                            commit()
                            true
                        }
                        Key.Escape -> {
                            text = shown
                            invalid = false
                            true
                        }
                        else -> false
                    }
                },
            placeholder = placeholder,
            contentDescription = contentDescription,
            trailing = { FieldIcon(icon, contentDescription = openDescription, onClick = { open = true }) },
            error = if (invalid) invalidMessage else null,
        )
        Popover(open, { open = false }, width = popoverWidth) { picker { open = false } }
    }
}
