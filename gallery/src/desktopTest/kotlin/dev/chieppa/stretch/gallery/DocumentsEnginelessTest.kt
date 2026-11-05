package dev.chieppa.stretch.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test

/** The documents page on a runtime without JCEF, as the build arranges for tests without -Pcef. */
@OptIn(ExperimentalTestApi::class)
class DocumentsEnginelessTest {
    @Test
    fun everyTabDrawsWithoutTheEngine() = runComposeUiTest {
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Box(Modifier.size(1100.dp, 760.dp)) { DocumentsPage() }
            }
        }
        for (tab in listOf("Markdown", "Diff", "HTML")) {
            onNodeWithText(tab).performClick()
            waitForIdle()
        }
    }
}
