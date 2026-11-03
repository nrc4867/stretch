package dev.chieppa.stretch.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private val dayCell = 34.dp
private const val daysInWeek = 7

/** How wide a [Calendar] is, for fitting a panel around it. */
val CalendarWidth = dayCell * daysInWeek

/**
 * A month of days to pick from. Arrow keys move a day or a week, Page Up and Page Down a month, and Enter or Space
 * picks. Days outside [range] can't be picked. Days from the months on either side show muted and can be picked too.
 */
@Composable
fun Calendar(
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    previousDescription: String,
    nextDescription: String,
    modifier: Modifier = Modifier,
    range: ClosedRange<LocalDate>? = null,
    today: LocalDate = currentDate(),
    firstDay: DayOfWeek = firstDayOfWeek(),
) {
    val c = Stretch.colors
    var month by remember { mutableStateOf(firstOfMonth(selected ?: today)) }
    var cursor by remember { mutableStateOf(selected ?: today) }
    LaunchedEffect(selected) {
        if (selected != null) {
            month = firstOfMonth(selected)
            cursor = selected
        }
    }
    fun allowed(date: LocalDate) = range == null || date in range
    fun moveTo(date: LocalDate) {
        val target = if (range == null) date else date.coerceIn(range.start, range.endInclusive)
        cursor = target
        month = firstOfMonth(target)
    }

    val title = monthTitle(month)
    val rtl = isRtl()
    val grid = rememberControlState()
    val focus = remember { FocusRequester() }
    Column(modifier.width(CalendarWidth), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(StretchIcons.ChevronLeft, previousDescription, { month = month.minus(1, DateTimeUnit.MONTH) })
            Label(title, Stretch.type.body.copy(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center), c.inkStrong, Modifier.weight(1f))
            IconButton(StretchIcons.ChevronRight, nextDescription, { month = month.plus(1, DateTimeUnit.MONTH) })
        }
        Row {
            for (offset in 0 until daysInWeek) {
                val day = DayOfWeek.entries[(firstDay.ordinal + offset) % daysInWeek]
                Box(Modifier.size(dayCell), contentAlignment = Alignment.Center) { Label(weekdayName(day), Stretch.type.bodySm, c.inkMuted) }
            }
        }
        Column(
            Modifier
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (along(event.key, rtl)) {
                        Key.DirectionLeft -> moveTo(cursor.minus(1, DateTimeUnit.DAY))
                        Key.DirectionRight -> moveTo(cursor.plus(1, DateTimeUnit.DAY))
                        Key.DirectionUp -> moveTo(cursor.minus(daysInWeek, DateTimeUnit.DAY))
                        Key.DirectionDown -> moveTo(cursor.plus(daysInWeek, DateTimeUnit.DAY))
                        Key.PageUp -> moveTo(cursor.minus(1, DateTimeUnit.MONTH))
                        Key.PageDown -> moveTo(cursor.plus(1, DateTimeUnit.MONTH))
                        Key.Enter, Key.Spacebar -> if (allowed(cursor)) onSelect(cursor)
                        else -> return@onKeyEvent false
                    }
                    true
                }
                .focusRequester(focus)
                .focusable(interactionSource = grid.source)
                .semantics { contentDescription = title },
        ) {
            monthGrid(month, firstDay).chunked(daysInWeek).forEach { week ->
                Row {
                    for (date in week) {
                        PickCell(
                            text = date.day.toString(),
                            selected = date == selected,
                            onClick = {
                                moveTo(date)
                                onSelect(date)
                            },
                            modifier = Modifier.size(dayCell).padding(1.dp),
                            enabled = allowed(date),
                            muted = date.month != month.month,
                            cursor = grid.focused && FocusVisibility.keyboard && date == cursor,
                            marked = date == today,
                            description = formatDate(date),
                        )
                    }
                }
            }
        }
    }
}
