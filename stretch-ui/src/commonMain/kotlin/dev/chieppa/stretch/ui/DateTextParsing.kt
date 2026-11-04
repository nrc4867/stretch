package dev.chieppa.stretch.ui

import kotlinx.datetime.LocalDate

/** The fields a written date holds. */
internal enum class DateField { Year, Month, Day }

/** A year written with two digits falls in this century. */
private const val TWO_DIGIT_YEAR_BASE = 2000
private const val TWO_DIGIT_YEAR_LIMIT = 100

/**
 * Reads [text] as a date with its fields in [order]: the words and numbers in it, in that order, with the month a number
 * or a key of [monthNames] (lowercase, month numbered from 1). Words that name no month are ignored, so a locale's
 * literals such as "de" pass. Null if it isn't a date.
 */
internal fun parseDateFields(text: String, order: List<DateField>, monthNames: Map<String, Int>): LocalDate? {
    val fields = dateTokens(text).mapNotNull { token -> token.toIntOrNull() ?: monthNames[token.lowercase()] }
    if (fields.size != order.size) return null
    val values = order.zip(fields).toMap()
    val year = values[DateField.Year] ?: return null
    val month = values[DateField.Month] ?: return null
    val day = values[DateField.Day] ?: return null
    return runCatching {
        LocalDate(if (year < TWO_DIGIT_YEAR_LIMIT) TWO_DIGIT_YEAR_BASE + year else year, month, day)
    }.getOrNull()
}

/** The runs of letters and of digits in [text]. */
private fun dateTokens(text: String): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    fun flush() {
        if (current.isNotEmpty()) tokens += current.toString()
        current.clear()
    }
    for (c in text) {
        if (!c.isLetterOrDigit()) {
            flush()
            continue
        }
        if (current.isNotEmpty() && current.last().isDigit() != c.isDigit()) flush()
        current.append(c)
    }
    flush()
    return tokens
}
