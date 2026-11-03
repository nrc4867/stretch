package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import dev.chieppa.stretch.ui.ButtonKind
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class DateTimeInputsTest {
    private val before = Locale.getDefault()
    private val hasError = SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)

    @BeforeTest
    fun pinLocale() = Locale.setDefault(Locale.US)

    @AfterTest
    fun restoreLocale() = Locale.setDefault(before)

    @Test
    fun typingADateCommitsOnEnter() = runComposeUiTest {
        var date by mutableStateOf<LocalDate?>(null)
        setContent {
            StretchTheme(StretchThemes.Ink) { DateInput(date, { date = it }, "Due", Modifier.width(200.dp)) }
        }
        val field = onNodeWithContentDescription("Due")
        field.performTextInput("2026-11-02")
        assertNull(date)
        field.performKeyInput { pressKey(Key.Enter) }
        assertEquals(LocalDate(2026, 11, 2), date)
        onNodeWithText("Nov 2, 2026").assertExists()
    }

    @Test
    fun badTextShowsTheErrorAndKeepsTheValue() = runComposeUiTest {
        var date by mutableStateOf<LocalDate?>(LocalDate(2026, 11, 2))
        setContent {
            StretchTheme(StretchThemes.Ink) { DateInput(date, { date = it }, "Due", Modifier.width(200.dp)) }
        }
        val field = onNodeWithContentDescription("Due")
        field.performTextReplacement("soon")
        field.performKeyInput { pressKey(Key.Enter) }
        field.assert(hasError)
        assertEquals(LocalDate(2026, 11, 2), date)
        field.performKeyInput { pressKey(Key.Escape) }
        onNodeWithText("Nov 2, 2026").assertExists()
    }

    @Test
    fun clearingTheFieldClearsTheValue() = runComposeUiTest {
        var time by mutableStateOf<LocalTime?>(LocalTime(14, 20))
        setContent {
            StretchTheme(StretchThemes.Ink) { TimeInput(time, { time = it }, "Start", Modifier.width(200.dp), hour12 = false) }
        }
        val field = onNodeWithContentDescription("Start")
        field.performTextReplacement("")
        field.performKeyInput { pressKey(Key.Enter) }
        assertNull(time)
    }

    @Test
    fun aDateOutsideTheRangeIsAnError() = runComposeUiTest {
        var date by mutableStateOf<LocalDate?>(null)
        setContent {
            StretchTheme(StretchThemes.Ink) {
                DateInput(date, { date = it }, "Due", Modifier.width(200.dp), range = LocalDate(2026, 11, 1)..LocalDate(2026, 11, 30))
            }
        }
        val field = onNodeWithContentDescription("Due")
        field.performTextInput("2026-12-25")
        field.performKeyInput { pressKey(Key.Enter) }
        field.assert(hasError)
        assertNull(date)
    }

    @Test
    fun pickingFromThePopoverSetsTheDateAndCloses() = runComposeUiTest {
        var date by mutableStateOf<LocalDate?>(LocalDate(2026, 11, 2))
        setContent {
            StretchTheme(StretchThemes.Ink) { DateInput(date, { date = it }, "Due", Modifier.width(200.dp)) }
        }
        onNodeWithContentDescription("Open calendar").performClick()
        onNodeWithContentDescription("Nov 14, 2026").performClick()
        assertEquals(LocalDate(2026, 11, 14), date)
        onNodeWithContentDescription("Nov 14, 2026").assertDoesNotExist()
    }

    @Test
    fun dateTimeWaitsForBothParts() = runComposeUiTest {
        var moment by mutableStateOf<LocalDateTime?>(null)
        setContent {
            StretchTheme(StretchThemes.Ink) {
                DateTimeInput(moment, { moment = it }, "Date", "Time", Modifier.width(320.dp), hour12 = false)
            }
        }
        val date = onNodeWithContentDescription("Date")
        date.performTextInput("2026-11-02")
        date.performKeyInput { pressKey(Key.Enter) }
        assertNull(moment)
        onNodeWithText("Nov 2, 2026").assertExists()
        val time = onNodeWithContentDescription("Time")
        time.performTextInput("14:20")
        time.performKeyInput { pressKey(Key.Enter) }
        assertEquals(LocalDateTime(2026, 11, 2, 14, 20), moment)
    }

    @Test
    fun splitButtonOpensItsMenu() = runComposeUiTest {
        var main = 0
        var picked = 0
        setContent {
            StretchTheme(StretchThemes.Ink) {
                SplitButton("Export", { main++ }, "More", kind = ButtonKind.Primary) { close ->
                    MenuItem("Export as GIF", {
                        picked++
                        close()
                    })
                }
            }
        }
        onNodeWithText("Export").performClick()
        assertEquals(1, main)
        onNodeWithContentDescription("More").performClick()
        onNodeWithText("Export as GIF").performClick()
        assertEquals(1, picked)
        onNodeWithText("Export as GIF").assertDoesNotExist()
    }
}
