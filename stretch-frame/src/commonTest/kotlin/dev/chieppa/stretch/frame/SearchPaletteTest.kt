package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.Stretch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchPaletteTest {
    @Test
    fun scoresFollowTheRanking() {
        assertEquals(0, matchScore("rot", "Rotate refresh tokens"))
        assertEquals(1, matchScore("tok", "Rotate refresh tokens"))
        assertEquals(2, matchScore("tok", "useTokenRefresh.test.tsx"))
        assertEquals(3, matchScore("rrt", "Rotate refresh tokens"))
        assertNull(matchScore("zzz", "Rotate refresh tokens"))
    }

    @Test
    fun rangesMarkWhatMatched() {
        assertEquals(listOf(15..17), matchRanges("tok", "Rotate refresh tokens"))
        assertEquals(listOf(3..5), matchRanges("TOK", "useTokenRefresh"))
        assertEquals(listOf(0..1, 7..7), matchRanges("rof", "Rotate fresh"))
        assertEquals(emptyList(), matchRanges("zz", "Rotate"))
        assertEquals(emptyList(), matchRanges(" ", "Rotate"))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun keysReachTheAppAndEscCloses() = runComposeUiTest {
        var open by mutableStateOf(true)
        var query by mutableStateOf("")
        var downs by mutableIntStateOf(0)
        setStretchContent {
            if (open) {
                SearchPalette(
                    query = query,
                    onQueryChange = { query = it },
                    onDismiss = { open = false },
                    placeholder = "Switch to",
                    scrim = true,
                    hints = listOf(PaletteHint(listOf("↵"), "Switch")),
                    onKey = { if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionDown) { downs++; true } else false },
                ) {
                    Column {
                        PaletteSection("In this review", note = "2")
                        PaletteRow(highlighted = true, onClick = {}) {
                            MatchedText("Rotate refresh tokens", matchRanges(query, "Rotate refresh tokens"), Stretch.type.body, Stretch.colors.ink)
                        }
                    }
                }
            }
        }
        onNodeWithContentDescription("Switch to").performTextInput("tok")
        assertEquals("tok", query)
        onNodeWithText("IN THIS REVIEW").assertExists()
        onNodeWithText("Switch").assertExists()
        onNodeWithContentDescription("Switch to").performKeyInput { pressKey(Key.DirectionDown) }
        assertEquals(1, downs)
        onNodeWithContentDescription("Switch to").performKeyInput { pressKey(Key.Escape) }
        assertEquals(false, open)
    }
}
