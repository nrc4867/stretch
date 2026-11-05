package dev.chieppa.stretch.web

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.height
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** On a runtime without JCEF, as the build arranges for tests without -Pcef. */
@OptIn(ExperimentalTestApi::class)
class EnginelessTest {
    @Test
    fun reportsNoEngine() = assertFalse(WebEngine.available)

    @Test
    fun aViewShowsItsFallback() = runComposeUiTest {
        setContent {
            StretchTheme(StretchThemes.Ink) {
                HtmlView(HtmlContent.Inline("<p>Page</p>"), unavailable = { BasicText("No engine") })
            }
        }
        onNodeWithText("No engine").assertExists()
    }

    @Test
    fun aViewShowsAllOfTheDocumentSourceByDefault() = runComposeUiTest {
        val oneLine = "<p>Page</p>"
        val fiveLines = List(5) { oneLine }.joinToString("\n")
        var document by mutableStateOf(oneLine)
        setContent {
            StretchTheme(StretchThemes.Ink) { HtmlView(HtmlContent.Inline(document)) }
        }
        val oneLineHeight = onNodeWithText(oneLine).getBoundsInRoot().height
        document = fiveLines
        assertTrue(onNodeWithText(fiveLines).getBoundsInRoot().height > oneLineHeight * 4)
    }
}
