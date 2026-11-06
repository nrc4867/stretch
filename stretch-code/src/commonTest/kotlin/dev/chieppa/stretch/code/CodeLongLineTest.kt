package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CodeLongLineTest {
    private val huge = "{\"key\":\"" + "x".repeat(LONG_LINE_CHARS) + "\"}"

    @Test
    fun aLinesWidthStopsAtTheWidestALayoutCanBe() {
        assertEquals(80, codeWidthPx(charWidth = 8, longest = 8))
        assertEquals(MAX_CODE_WIDTH_PX, codeWidthPx(charWidth = 8, longest = LONG_LINE_CHARS))
        assertEquals(MAX_CODE_WIDTH_PX, codeWidthPx(charWidth = 8, longest = Int.MAX_VALUE))
    }

    @Test
    fun aViewerShowsATextWithAVeryLongLine() = runComposeUiTest {
        setStretchContent(StretchThemes.Ink) {
            CodeViewer(CodeText(listOf("first", huge, "last")), Modifier.size(400.dp, 300.dp))
        }
        onNodeWithText("first").assertIsDisplayed()
        onNodeWithText("last").assertIsDisplayed()
    }

    private companion object {
        const val LONG_LINE_CHARS = 200_000
    }
}
