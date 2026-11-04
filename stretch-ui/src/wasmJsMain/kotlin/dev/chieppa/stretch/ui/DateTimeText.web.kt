@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.ui

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number

private const val MONTHS_IN_YEAR = 12

/** A weekday's name is written with at most this many letters in a calendar's column heads. */
private const val WEEKDAY_NAME_LENGTH = 2

/** Monday, January 1: the seven days from it give every weekday in ISO order. */
private const val MONDAY_YEAR = 2024
private const val MONDAY_MONTH = 1
private const val MONDAY_DAY = 1

/** Hours that show the words for before and after noon. */
private const val MIDNIGHT = 0
private const val NOON = 12

private const val ISO_DATE_LENGTH = 10

private fun firstDayNumber(): Int = js("""(() => {
    const locale = new Intl.Locale(navigator.language);
    const info = locale.getWeekInfo ? locale.getWeekInfo() : locale.weekInfo;
    return info ? info.firstDay : 1;
})()""")

private fun hourCycleIsTwelve(): Boolean =
    js("new Intl.DateTimeFormat(navigator.language, { hour: 'numeric' }).resolvedOptions().hour12 === true")

private fun formatMonthTitle(year: Int, month: Int): String = js("""new Intl.DateTimeFormat(navigator.language, { month: 'long', year: 'numeric', timeZone: 'UTC' })
    .format(new Date(Date.UTC(year, month - 1, 1)))""")

private fun formatShortWeekday(year: Int, month: Int, day: Int): String = js("""new Intl.DateTimeFormat(navigator.language, { weekday: 'short', timeZone: 'UTC' })
    .format(new Date(Date.UTC(year, month - 1, day)))""")

private fun formatDayPeriod(hour: Int): String = js("""(() => {
    const parts = new Intl.DateTimeFormat(navigator.language, { hour: 'numeric', hour12: true, timeZone: 'UTC' })
        .formatToParts(new Date(Date.UTC(2024, 0, 1, hour)));
    const period = parts.find((part) => part.type === 'dayPeriod');
    return period ? period.value : '';
})()""")

private fun formatMediumDate(year: Int, month: Int, day: Int): String = js("""new Intl.DateTimeFormat(navigator.language, { dateStyle: 'medium', timeZone: 'UTC' })
    .format(new Date(Date.UTC(year, month - 1, day)))""")

/** The locale's short date's fields in order, joined with commas. */
private fun dateFieldOrder(): String = js("""new Intl.DateTimeFormat(navigator.language, { dateStyle: 'short', timeZone: 'UTC' })
    .formatToParts(new Date(Date.UTC(2024, 0, 1)))
    .filter((part) => ['year', 'month', 'day'].includes(part.type))
    .map((part) => part.type)
    .join(',')""")

private fun monthName(month: Int, style: String): String = js("""new Intl.DateTimeFormat(navigator.language, { month: style, timeZone: 'UTC' })
    .format(new Date(Date.UTC(2024, month - 1, 1)))""")

actual fun firstDayOfWeek(): DayOfWeek = DayOfWeek(firstDayNumber())

actual fun usesTwelveHourClock(): Boolean = hourCycleIsTwelve()

actual fun monthTitle(date: LocalDate): String = formatMonthTitle(date.year, date.month.number)

actual fun weekdayName(day: DayOfWeek): String =
    formatShortWeekday(MONDAY_YEAR, MONDAY_MONTH, MONDAY_DAY + day.isoDayNumber - 1).take(WEEKDAY_NAME_LENGTH)

actual fun dayPeriodNames(): Pair<String, String> = formatDayPeriod(MIDNIGHT) to formatDayPeriod(NOON)

actual fun formatDate(date: LocalDate): String = formatMediumDate(date.year, date.month.number, date.day)

actual fun parseDate(text: String): LocalDate? {
    val trimmed = text.trim()
    if (trimmed.length == ISO_DATE_LENGTH) runCatching { return LocalDate.parse(trimmed) }
    val order = dateFieldOrder().split(',').map { field ->
        when (field) {
            "year" -> DateField.Year
            "month" -> DateField.Month
            else -> DateField.Day
        }
    }
    val names = buildMap {
        for (month in 1..MONTHS_IN_YEAR) {
            put(monthName(month, "long").lowercase(), month)
            put(monthName(month, "short").trimEnd('.').lowercase(), month)
        }
    }
    return parseDateFields(trimmed, order, names)
}
