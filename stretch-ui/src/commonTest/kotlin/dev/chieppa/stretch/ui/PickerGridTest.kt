package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PickerGridTest {
    // Three columns at 256dp: Torso Leg Arm / Paw Foot Hand / Head
    private val parts = listOf("Torso", "Leg", "Arm", "Paw", "Foot", "Hand", "Head")

    private fun ComposeUiTest.show(
        cursor: () -> String?,
        onCursor: (String?) -> Unit,
        picked: MutableList<String>,
        enabled: (String) -> Boolean = { true },
        onLeaveTop: () -> Unit = {},
        gridModifier: Modifier = Modifier,
        above: @Composable () -> Unit = {},
    ) = setContent {
        StretchTheme(StretchThemes.Ink) {
            Column {
                above()
                PickerGrid(
                    parts, cursor(), onCursor, { picked += it }, "Parts",
                    gridModifier.width(256.dp).testTag("grid"),
                    enabled = enabled, onLeaveTop = onLeaveTop,
                ) { part, _ -> BasicText(part, Modifier.fillMaxWidth().height(40.dp)) }
            }
        }
    }

    @Test
    fun arrowsMoveTheCursorAndEnterPicks() = runComposeUiTest {
        var cursor by mutableStateOf<String?>(null)
        val picked = mutableListOf<String>()
        show({ cursor }, { cursor = it }, picked)
        onNodeWithTag("grid").requestFocus()
        onNodeWithTag("grid").performKeyInput { pressKey(Key.DirectionDown) }
        assertEquals("Torso", cursor)
        onNodeWithTag("grid").performKeyInput {
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionDown)
        }
        assertEquals("Foot", cursor)
        onNodeWithTag("grid").performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf("Foot"), picked)
    }

    @Test
    fun disabledCellsAreSkippedAndNeverPicked() = runComposeUiTest {
        var cursor by mutableStateOf<String?>("Torso")
        val picked = mutableListOf<String>()
        show({ cursor }, { cursor = it }, picked, enabled = { it != "Leg" })
        onNodeWithTag("grid").requestFocus()
        onNodeWithTag("grid").performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals("Arm", cursor)
        onNodeWithText("Leg").performClick()
        assertEquals("Arm", cursor)
    }

    @Test
    fun aClickMovesTheCursorAndADoubleClickPicks() = runComposeUiTest {
        var cursor by mutableStateOf<String?>(null)
        val picked = mutableListOf<String>()
        show({ cursor }, { cursor = it }, picked)
        onNodeWithText("Hand").performClick()
        assertEquals("Hand", cursor)
        assertEquals(emptyList(), picked)
        onNodeWithText("Paw").performMouseInput { doubleClick() }
        assertEquals("Paw", cursor)
        assertEquals(listOf("Paw"), picked)
    }

    @Test
    fun theCursorCellIsMarkedSelected() = runComposeUiTest {
        show({ "Foot" }, {}, mutableListOf())
        onNodeWithText("Foot").assertIsSelected()
    }

    @Test
    fun enterWithNoCursorIsLeftToTheContentAround() = runComposeUiTest {
        var outer = 0
        val picked = mutableListOf<String>()
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Column(
                    Modifier.onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) outer++
                        false
                    },
                ) {
                    PickerGrid(parts, null, {}, { picked += it }, "Parts", Modifier.width(256.dp).testTag("grid")) { part, _ ->
                        BasicText(part, Modifier.height(40.dp))
                    }
                }
            }
        }
        onNodeWithTag("grid").requestFocus()
        onNodeWithTag("grid").performKeyInput { pressKey(Key.Enter) }
        assertEquals(emptyList(), picked)
        assertEquals(1, outer)
    }

    @Test
    fun upFromTheFirstRowGoesToTheSearchFieldAndDownComesBackToTheBestMatch() = runComposeUiTest {
        var cursor by mutableStateOf<String?>("Leg")
        val search = FocusRequester()
        val grid = FocusRequester()
        show(
            { cursor }, { cursor = it }, mutableListOf(),
            onLeaveTop = { search.requestFocus() },
            gridModifier = Modifier.focusRequester(grid),
            above = {
                var query by remember { mutableStateOf("") }
                BasicTextField(
                    query, { query = it },
                    Modifier.testTag("search").focusRequester(search).onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                            cursor = "Hand"
                            grid.requestFocus()
                            true
                        } else {
                            false
                        }
                    },
                )
            },
        )
        onNodeWithTag("grid").requestFocus()
        onNodeWithTag("grid").performKeyInput { pressKey(Key.DirectionUp) }
        onNodeWithTag("search").assertIsFocused()
        onNodeWithTag("search").performKeyInput { pressKey(Key.DirectionDown) }
        onNodeWithTag("grid").assertIsFocused()
        assertEquals("Hand", cursor)
    }

    @Test
    fun theCursorScrollsIntoViewInTheScrollingColumnAround() = runComposeUiTest {
        var cursor by mutableStateOf<String?>("Torso")
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Column(Modifier.heightIn(max = 60.dp).verticalScroll(rememberScrollState())) {
                    PickerGrid(parts, cursor, { cursor = it }, {}, "Parts", Modifier.width(256.dp)) { part, _ ->
                        BasicText(part, Modifier.height(40.dp))
                    }
                }
            }
        }
        onNodeWithText("Head").assertIsNotDisplayed()
        cursor = "Head"
        waitForIdle()
        onNodeWithText("Head").assertIsDisplayed()
    }
}
