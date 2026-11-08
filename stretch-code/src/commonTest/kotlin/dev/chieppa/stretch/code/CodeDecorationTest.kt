package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.annotatedText
import dev.chieppa.stretch.test.characterCenter
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import dev.chieppa.stretch.ui.TOOLTIP_DELAY_MS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class CodeDecorationTest {
    private val look = CodeDecorationLook(Color.Cyan, Color.Blue, Color.Yellow, Color.Magenta, Color.Red, Color.Green)

    private fun deco(start: Int, end: Int, style: CodeDecorationStyle, tooltip: String? = null) = CodeDecoration(CodeRange(1, start, end), style, tooltip)

    @Test
    fun eachCharacterKeepsTheStrongestRunOverIt() {
        val runs = strongestRuns(10, listOf(RankedRun(0, 6, 0), RankedRun(4, 8, 2), RankedRun(7, 9, 1)))
        assertEquals(listOf(RankedRun(0, 4, 0), RankedRun(4, 8, 2), RankedRun(8, 9, 1)), runs)
        assertEquals(listOf(RankedRun(2, 10, 1)), strongestRuns(10, listOf(RankedRun(2, 5, 1), RankedRun(5, 40, 1))))
    }

    @Test
    fun washesAndUnderlinesComposeOnTheirOwnLayers() {
        val paint = decorationPaint(
            "\tvalue = other",
            listOf(
                deco(1, 6, CodeDecorationStyle.Reference),
                deco(1, 3, CodeDecorationStyle.Definition),
                deco(0, 14, CodeDecorationStyle.Accent),
                deco(9, 14, CodeDecorationStyle.Warning),
                deco(11, 12, CodeDecorationStyle.Error),
            ),
            look,
        )
        assertEquals(listOf(CodeHighlight(1, 3, Color.Blue), CodeHighlight(3, 6, Color.Cyan)), paint.washes)
        // Underlines count in the text as shown, the tab four columns wide.
        assertEquals(
            listOf(
                CodeUnderline(0, 12, Color.Yellow, UnderlineShape.Dotted),
                CodeUnderline(12, 14, Color.Magenta, UnderlineShape.Wavy),
                CodeUnderline(14, 15, Color.Red, UnderlineShape.Wavy),
                CodeUnderline(15, 17, Color.Magenta, UnderlineShape.Wavy),
            ),
            paint.underlines,
        )
    }

    @Test
    fun tooltipsThatMeetShowTogetherMostSevereFirst() {
        val decorations = listOf(
            deco(0, 5, CodeDecorationStyle.Reference, "Read here"),
            deco(2, 8, CodeDecorationStyle.Error, "Unresolved"),
            deco(0, 9, CodeDecorationStyle.Accent),
        )
        assertEquals(DecorationTip("Unresolved\nRead here", CodeRange(1, 2, 8)), decorationTip(decorations, 3))
        assertEquals(DecorationTip("Read here", CodeRange(1, 0, 5)), decorationTip(decorations, 0))
        assertNull(decorationTip(decorations, 8))
    }

    @Test
    fun decorationsWashTheirTextAndShowTheirTooltipOnHover() = runComposeUiTest {
        val decorations = listOf(
            CodeDecoration(CodeRange(2, 4, 9), CodeDecorationStyle.Definition),
            CodeDecoration(CodeRange(1, 0, 3), CodeDecorationStyle.Warning, "Unused value"),
        )
        setContent {
            StretchTheme(StretchThemes.Ink) {
                CodeViewer(CodeText.of("val first = 1\nval second = 2"), Modifier.size(600.dp, 300.dp), decorations = decorations)
            }
        }
        val washed = onNodeWithText("val second = 2").annotatedText().spanStyles.single { it.item.background != Color.Unspecified }
        assertEquals(4 to 9, washed.start to washed.end)
        onNodeWithText("Unused value").assertDoesNotExist()
        val first = onNodeWithText("val first = 1")
        first.performMouseInput { moveTo(first.characterCenter(1)) }
        mainClock.advanceTimeBy(TOOLTIP_WAIT_MS)
        onNodeWithText("Unused value").assertExists()
        first.performMouseInput { moveTo(first.characterCenter(6)) }
        mainClock.advanceTimeBy(TOOLTIP_WAIT_MS)
        onNodeWithText("Unused value").assertDoesNotExist()
    }
}

/** Longer than a tooltip waits before it shows. */
private const val TOOLTIP_WAIT_MS = TOOLTIP_DELAY_MS + 100
