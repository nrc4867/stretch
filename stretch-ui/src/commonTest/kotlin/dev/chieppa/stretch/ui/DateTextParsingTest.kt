package dev.chieppa.stretch.ui

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DateTextParsingTest {
    private val monthFirst = listOf(DateField.Month, DateField.Day, DateField.Year)
    private val dayFirst = listOf(DateField.Day, DateField.Month, DateField.Year)
    private val yearFirst = listOf(DateField.Year, DateField.Month, DateField.Day)
    private val names = mapOf("nov" to 11, "november" to 11, "noviembre" to 11)

    @Test
    fun readsANamedMonth() = assertEquals(LocalDate(2026, 11, 2), parseDateFields("Nov 2, 2026", monthFirst, names))

    @Test
    fun readsNumbersInTheLocalesOrder() {
        assertEquals(LocalDate(2026, 11, 2), parseDateFields("11/2/26", monthFirst, names))
        assertEquals(LocalDate(2026, 11, 2), parseDateFields("2.11.2026", dayFirst, names))
    }

    @Test
    fun ignoresLiteralWords() {
        assertEquals(LocalDate(2026, 11, 2), parseDateFields("2 de noviembre de 2026", dayFirst, names))
        assertEquals(LocalDate(2026, 11, 2), parseDateFields("2026\u5E7411\u67082\u65E5", yearFirst, names))
    }

    @Test
    fun rejectsWhatIsNotADate() {
        assertNull(parseDateFields("soon", monthFirst, names))
        assertNull(parseDateFields("13/40/2026", monthFirst, names))
        assertNull(parseDateFields("11/2", monthFirst, names))
    }
}
