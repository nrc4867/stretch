package dev.chieppa.stretch.ui

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

private const val daysInGrid = 42
private const val daysInWeek = 7
private const val noon = 12

/** The day the OS's locale starts its week on. */
expect fun firstDayOfWeek(): DayOfWeek

/** Whether the OS's locale writes times with AM and PM. */
expect fun usesTwelveHourClock(): Boolean

/** The locale's month and year, for a calendar's title: "November 2026". */
expect fun monthTitle(date: LocalDate): String

/** The locale's shortest name for a weekday, for a calendar's column heads. */
expect fun weekdayName(day: DayOfWeek): String

/** The locale's words for before and after noon. */
expect fun dayPeriodNames(): Pair<String, String>

/** A date as the locale writes it in the medium style: "Nov 2, 2026". */
expect fun formatDate(date: LocalDate): String

/** Reads a date written as `2026-11-02` or in the locale's medium or short style; null if it isn't one. */
expect fun parseDate(text: String): LocalDate?

/** Today in the OS's time zone. */
fun currentDate(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

/** The first day of [date]'s month. */
fun firstOfMonth(date: LocalDate): LocalDate = LocalDate(date.year, date.month.number, 1)

/** The six weeks a calendar shows for [month]: always 42 days, so its height doesn't change between months. */
fun monthGrid(month: LocalDate, firstDay: DayOfWeek): List<LocalDate> {
    val first = firstOfMonth(month)
    val lead = (first.dayOfWeek.isoDayNumber - firstDay.isoDayNumber + daysInWeek) % daysInWeek
    val start = first.minus(lead, DateTimeUnit.DAY)
    return List(daysInGrid) { start.plus(it, DateTimeUnit.DAY) }
}

/** A time as `14:20`, or `2:20 PM` with [hour12]. */
fun formatTime(time: LocalTime, hour12: Boolean = usesTwelveHourClock()): String {
    val minutes = time.minute.toString().padStart(2, '0')
    if (!hour12) return "${time.hour.toString().padStart(2, '0')}:$minutes"
    val (am, pm) = dayPeriodNames()
    return "${twelveHour(time.hour)}:$minutes ${if (time.hour < noon) am else pm}"
}

private val timePattern = Regex("""(\d{1,2})(?::?(\d{2}))?\s*(am?|pm?)?""")

/** Reads a time such as `14:20`, `1420`, `2:20 pm` or `2p`; null if it isn't one. */
fun parseTime(text: String): LocalTime? {
    val (am, pm) = dayPeriodNames()
    val cleaned = text.trim().lowercase().replace(am.lowercase(), "am").replace(pm.lowercase(), "pm").replace(".", "")
    val match = timePattern.matchEntire(cleaned) ?: return null
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].ifEmpty { "0" }.toInt()
    val period = match.groupValues[3]
    if (minute >= 60) return null
    val hour24 = when {
        period.isEmpty() -> hour.takeIf { it < 24 }
        hour !in 1..noon -> null
        else -> to24Hour(hour, period.startsWith("p"))
    } ?: return null
    return LocalTime(hour24, minute)
}

/** The hour as a twelve-hour clock shows it. */
internal fun twelveHour(hour: Int): Int = (hour % noon).let { if (it == 0) noon else it }

/** A twelve-hour clock's hour as an hour of the day. */
internal fun to24Hour(hour: Int, pm: Boolean): Int = hour % noon + if (pm) noon else 0
