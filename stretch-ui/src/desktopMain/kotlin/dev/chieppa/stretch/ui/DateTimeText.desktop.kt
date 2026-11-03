package dev.chieppa.stretch.ui

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaDayOfWeek
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinDayOfWeek
import kotlinx.datetime.toKotlinLocalDate
import java.text.DateFormatSymbols
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private fun locale() = Locale.getDefault(Locale.Category.FORMAT)

actual fun firstDayOfWeek(): DayOfWeek = WeekFields.of(locale()).firstDayOfWeek.toKotlinDayOfWeek()

actual fun usesTwelveHourClock(): Boolean =
    DateTimeFormatterBuilder.getLocalizedDateTimePattern(null, FormatStyle.SHORT, IsoChronology.INSTANCE, locale()).contains('a')

actual fun monthTitle(date: LocalDate): String = DateTimeFormatter.ofPattern("LLLL y", locale()).format(date.toJavaLocalDate())

actual fun weekdayName(day: DayOfWeek): String = day.toJavaDayOfWeek().getDisplayName(TextStyle.SHORT_STANDALONE, locale()).take(2)

actual fun dayPeriodNames(): Pair<String, String> = DateFormatSymbols.getInstance(locale()).amPmStrings.let { it[0] to it[1] }

actual fun formatDate(date: LocalDate): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale()).format(date.toJavaLocalDate())

actual fun parseDate(text: String): LocalDate? {
    val trimmed = text.trim()
    val formats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale()),
        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale()),
    )
    for (format in formats) {
        try {
            return java.time.LocalDate.parse(trimmed, format).toKotlinLocalDate()
        } catch (_: DateTimeParseException) {
            // Try the next style.
        }
    }
    return null
}
