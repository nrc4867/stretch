package dev.chieppa.stretch.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Pins the locale, since dates and times read and write in the OS's. */
@OptIn(ExperimentalTestApi::class)
class DateTimeTest {
    private val before = Locale.getDefault()

    @BeforeTest
    fun pinLocale() = Locale.setDefault(Locale.US)

    @AfterTest
    fun restoreLocale() = Locale.setDefault(before)

    @Test
    fun monthGridStartsOnTheFirstDayAndAlwaysHasSixWeeks() {
        val sunday = monthGrid(LocalDate(2026, 11, 15), DayOfWeek.SUNDAY)
        assertEquals(42, sunday.size)
        assertEquals(LocalDate(2026, 11, 1), sunday.first())
        val monday = monthGrid(LocalDate(2026, 11, 15), DayOfWeek.MONDAY)
        assertEquals(LocalDate(2026, 10, 26), monday.first())
        assertEquals(DayOfWeek.MONDAY, monday.first().dayOfWeek)
    }

    @Test
    fun datesRoundTripAndBadOnesAreNull() {
        val date = LocalDate(2026, 11, 2)
        assertEquals("Nov 2, 2026", formatDate(date))
        assertEquals(date, parseDate("Nov 2, 2026"))
        assertEquals(date, parseDate("2026-11-02"))
        assertEquals(date, parseDate("11/2/26"))
        assertNull(parseDate("soon"))
        assertNull(parseDate("2026-13-01"))
    }

    @Test
    fun timesReadAndWrite() {
        assertEquals("14:20", formatTime(LocalTime(14, 20), hour12 = false))
        assertEquals("2:20 PM", formatTime(LocalTime(14, 20), hour12 = true))
        assertEquals("12:05 AM", formatTime(LocalTime(0, 5), hour12 = true))
        assertEquals(LocalTime(14, 20), parseTime("14:20"))
        assertEquals(LocalTime(14, 20), parseTime("1420"))
        assertEquals(LocalTime(14, 20), parseTime("2:20 pm"))
        assertEquals(LocalTime(14, 0), parseTime("2p"))
        assertEquals(LocalTime(0, 0), parseTime("12 AM"))
        assertEquals(LocalTime(12, 0), parseTime("12pm"))
        assertEquals(LocalTime(9, 30), parseTime("930"))
        assertNull(parseTime("25:00"))
        assertNull(parseTime("13 pm"))
        assertNull(parseTime("10:75"))
        assertNull(parseTime("lunch"))
    }

    @Test
    fun calendarPicksByClickAndByKey() = runComposeUiTest {
        var date by mutableStateOf<LocalDate?>(LocalDate(2026, 11, 2))
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Calendar(date, { date = it }, "Previous month", "Next month", today = LocalDate(2026, 11, 2), firstDay = DayOfWeek.SUNDAY)
            }
        }
        onNodeWithText("November 2026").assertExists()
        onNodeWithContentDescription("Nov 14, 2026").performClick()
        assertEquals(LocalDate(2026, 11, 14), date)
        onNodeWithContentDescription("Nov 14, 2026").assertIsSelected()

        onNodeWithContentDescription("November 2026").requestFocus().performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.DirectionRight)
            pressKey(Key.Enter)
        }
        assertEquals(LocalDate(2026, 11, 22), date)

        onNodeWithContentDescription("Next month").performClick()
        onNodeWithText("December 2026").assertExists()
    }

    @Test
    fun calendarKeepsToItsRange() = runComposeUiTest {
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Calendar(
                    LocalDate(2026, 11, 10), {}, "Previous month", "Next month",
                    range = LocalDate(2026, 11, 5)..LocalDate(2026, 11, 20),
                    today = LocalDate(2026, 11, 10), firstDay = DayOfWeek.SUNDAY,
                )
            }
        }
        onNodeWithContentDescription("Nov 4, 2026").assertIsNotEnabled()
        onNodeWithContentDescription("Nov 21, 2026").assertIsNotEnabled()
    }

    @Test
    fun timePickerChangesOnlyWhatWasPicked() = runComposeUiTest {
        var time by mutableStateOf(LocalTime(14, 20))
        setContent {
            StretchTheme(StretchThemes.Ink) {
                TimePicker(time, { time = it }, "Hour", "Minute", hour12 = true)
            }
        }
        onNodeWithText("AM").performClick()
        assertEquals(LocalTime(2, 20), time)
        onNodeWithText("PM").performClick()
        assertEquals(LocalTime(14, 20), time)
        onAllNodesWithText("03").onFirst().performClick()
        assertEquals(LocalTime(15, 20), time)
        onNodeWithContentDescription("Minute").requestFocus().performKeyInput { pressKey(Key.DirectionDown) }
        assertEquals(LocalTime(15, 25), time)
    }

    @Test
    fun timePickerKeepsAMinuteBetweenSteps() = runComposeUiTest {
        setContent {
            StretchTheme(StretchThemes.Ink) {
                TimePicker(LocalTime(9, 23), {}, "Hour", "Minute", hour12 = false)
            }
        }
        onAllNodesWithText("23")[1].assertIsSelected()
    }
}
