package dev.chieppa.stretch.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.geometry.Offset
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TimelineTest {
    @Test
    fun rulerMath() {
        assertEquals(3, frameAt(45f, 14f, 48))
        assertEquals(47, frameAt(9000f, 14f, 48))
        assertEquals(2, rulerStep(14f, 28f))
        assertEquals(5, rulerStep(6f, 28f))
        assertEquals(10, rulerStep(3f, 28f))
    }

    @Test
    fun scrubsStepsPlaysAndRetimesKeys() = runComposeUiTest {
        var frame by mutableIntStateOf(0)
        var playing by mutableStateOf(false)
        var tracks by mutableStateOf(listOf(TimelineTrack("head", "Head", listOf(2, 10))))
        var selected by mutableStateOf(setOf<TimelineKey>())
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Timeline(
                    24, frame, { frame = it }, tracks, playing, { playing = it },
                    selected = selected, onSelectedChange = { selected = it },
                    onMoveKeys = { keys, d -> tracks = tracks.map { t -> t.copy(keys = t.keys.map { k -> if (TimelineKey(t.id, k) in keys) k + d else k }) } },
                )
            }
        }
        onNodeWithContentDescription("Frame").performTouchInput { click(Offset(14f * 5 + 3, centerY)) }
        assertEquals(5, frame)
        onNodeWithContentDescription("Frame").performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(6, frame)
        onNodeWithContentDescription("Play").performClick()
        assertTrue(playing)
        onNodeWithContentDescription("Head: 3, 11").performTouchInput {
            down(Offset(14f * 2 + 7, centerY))
            repeat(6) { moveBy(Offset(7f, 0f)) }
            up()
        }
        assertEquals(setOf(TimelineKey("head", 2)), selected)
        assertEquals(listOf(5, 10), tracks.first().keys)
    }
}
