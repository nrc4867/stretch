package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DataTest {
    @Test
    fun compactNumbers() {
        assertEquals("1,284", compactNumber(1284.0))
        assertEquals("12.9K", compactNumber(12_900.0))
        assertEquals("4.2M", compactNumber(4_200_000.0))
        assertEquals("-30K", compactNumber(-30_000.0))
        assertEquals("2.5", compactNumber(2.5))
    }

    @Test
    fun ticksAreCleanAndCoverTheData() {
        assertEquals(listOf(0f, 10f, 20f, 30f), niceTicks(8f, 21f))
        assertEquals(listOf(0f, 20f, 40f, 60f), niceTicks(18f, 42f))
        assertEquals(listOf(-10f, -5f, 0f, 5f, 10f, 15f), niceTicks(-6f, 14f))
    }

    @Test
    fun dragMath() {
        assertEquals(2, dropIndex(0, 70f, 34f, 5))
        assertEquals(0, dropIndex(1, -200f, 34f, 5))
        assertEquals(4, dropIndex(3, 1000f, 34f, 5))
        assertEquals(5, gridIndexAt(170f, 90f, 80f, 3, 6))
        assertEquals(listOf("b", "c", "a"), listOf("a", "b", "c").moved(0, 2))
        assertEquals(listOf("c", "a", "b"), listOf("a", "b", "c").moved(2, 0))
        assertEquals(listOf(10f, 22f, 34f), spreadLabels(listOf(10f, 14f, 20f), 12f))
    }

    @Test
    fun filesAreAcceptedByExtension() {
        val images = setOf("png", "jpg", "svg")
        assertEquals("png", fileExtension("C:\\refs\\Pose.PNG"))
        assertEquals("", fileExtension("/home/me/.config/notes"))
        assertTrue(acceptsFiles(listOf("a.png", "b.svg"), images))
        assertFalse(acceptsFiles(listOf("a.png", "b.txt"), images))
        assertFalse(acceptsFiles(emptyList(), images))
        assertTrue(acceptsFiles(listOf("anything.bin"), emptySet()))
    }

    @Test
    fun seriesOutsideThePaletteFail() {
        assertFailsWith<IllegalArgumentException> { ChartSeries("Ninth", listOf(1f), 8) }
    }

    private data class Part(val name: String, val strokes: Int)

    private val parts = listOf(Part("Body", 1284), Part("Head", 905), Part("Tail", 96))
    private val columns = listOf(
        TableColumn<Part>("Part", sortBy = { it.name }) { it.name },
        TableColumn<Part>("Strokes", numeric = true, sortBy = { it.strokes }) { groupThousands(it.strokes.toLong()) },
    )

    @Test
    fun rowsSortStablyBothWays() {
        assertEquals(listOf("Tail", "Head", "Body"), sortRows(parts, columns, TableSort(1)).map { it.name })
        assertEquals(listOf("Body", "Head", "Tail"), sortRows(parts, columns, TableSort(1, ascending = false)).map { it.name })
        assertEquals(parts, sortRows(parts, columns, null))
    }

    @Test
    fun tableSortsFromTheHeaderAndSelectsRows() = runComposeUiTest {
        var sort by mutableStateOf<TableSort?>(null)
        var selected by mutableStateOf<Part?>(null)
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Table(parts, columns, Modifier.width(400.dp), sort = sort, onSortChange = { sort = it }, isSelected = { it == selected }, onRowClick = { selected = it })
            }
        }
        onNodeWithText("STROKES").performClick()
        assertEquals(TableSort(1), sort)
        onNodeWithText("STROKES").performClick()
        assertEquals(TableSort(1, ascending = false), sort)
        onNodeWithText("Head").performClick()
        assertEquals("Head", selected?.name)
        onNodeWithText("Head", useUnmergedTree = true).assertExists()
    }

    @Test
    fun sortableListMovesWithAltArrows() = runComposeUiTest {
        var items by mutableStateOf(listOf("Body", "Head", "Tail"))
        var moves = 0
        setContent {
            StretchTheme(StretchThemes.Ink) {
                SortableList(items, { it }, { from, to -> moves++; items = items.moved(from, to) }, Modifier.width(240.dp), onClick = {}) {
                    androidx.compose.foundation.text.BasicText(it)
                }
            }
        }
        onNodeWithText("Tail").requestFocus()
        onNodeWithText("Tail").performKeyInput { withKeyDown(Key.AltLeft) { pressKey(Key.DirectionUp) } }
        assertEquals(listOf("Body", "Tail", "Head"), items)
        onNodeWithText("Tail").performKeyInput { withKeyDown(Key.AltLeft) { pressKey(Key.DirectionUp) } }
        assertEquals(listOf("Tail", "Body", "Head"), items)
        onNodeWithText("Tail").performKeyInput { withKeyDown(Key.AltLeft) { pressKey(Key.DirectionUp) } }
        assertEquals(2, moves)
    }

    @Test
    fun draggingARowIsOneMove() = runComposeUiTest {
        var items by mutableStateOf(listOf("Body", "Head", "Tail", "Paws"))
        val moves = mutableListOf<Pair<Int, Int>>()
        setContent {
            StretchTheme(StretchThemes.Ink) {
                SortableList(items, { it }, { from, to -> moves += from to to; items = items.moved(from, to) }, Modifier.width(240.dp), handles = false) {
                    androidx.compose.foundation.text.BasicText(it)
                }
            }
        }
        onNodeWithText("Body").performTouchInput {
            down(center)
            repeat(15) { moveBy(Offset(0f, 5f)) }
            up()
        }
        assertEquals(listOf(0 to 2), moves)
        assertEquals(listOf("Head", "Tail", "Body", "Paws"), items)
    }

    @Test
    fun mouseDragsRowsToo() = runComposeUiTest {
        var items by mutableStateOf(listOf("Body", "Head", "Tail", "Paws"))
        setContent {
            StretchTheme(StretchThemes.Ink) {
                SortableList(items, { it }, { from, to -> items = items.moved(from, to) }, Modifier.width(240.dp), handles = false) {
                    androidx.compose.foundation.text.BasicText(it)
                }
            }
        }
        onNodeWithText("Body").performMouseInput {
            moveTo(center)
            press()
            repeat(15) { moveBy(Offset(0f, 5f)) }
            release()
        }
        assertEquals(listOf("Head", "Tail", "Body", "Paws"), items)
    }

    @Test
    fun escapeCancelsADrag() = runComposeUiTest {
        var items by mutableStateOf(listOf("Body", "Head", "Tail"))
        setContent {
            StretchTheme(StretchThemes.Ink) {
                SortableList(items, { it }, { from, to -> items = items.moved(from, to) }, Modifier.width(240.dp), handles = false) {
                    androidx.compose.foundation.text.BasicText(it)
                }
            }
        }
        onNodeWithText("Body").performTouchInput {
            down(center)
            repeat(15) { moveBy(Offset(0f, 5f)) }
        }
        onAllNodesWithText("Body")[0].performKeyInput { pressKey(Key.Escape) }
        onAllNodesWithText("Body")[0].performTouchInput { up() }
        assertEquals(listOf("Body", "Head", "Tail"), items)
    }

    @Test
    fun dropzoneClickOpensThePicker() = runComposeUiTest {
        var browsed = 0
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Dropzone("Drop reference images", "PNG, JPG, SVG", setOf("png"), {}, { browsed++ }, "Release to import", "Not an image")
            }
        }
        onNodeWithText("PNG, JPG, SVG").assertExists()
        onNodeWithContentDescription("Drop reference images").performClick()
        assertEquals(1, browsed)
    }

    @Test
    fun chartsCarryLegendTooltipTextAndTableView() = runComposeUiTest {
        var table by mutableStateOf(false)
        val series = listOf(ChartSeries("Front paws", listOf(8f, 13f, 21f), 0), ChartSeries("Head", listOf(5f, 7f, 13f), 1))
        setContent {
            StretchTheme(StretchThemes.InkDark) {
                LineChart("Strokes per day", series, listOf("Day 1", "Day 2", "Day 3"), Modifier.width(320.dp), description = "Strokes per day, rising", table = table)
            }
        }
        onNodeWithText("Front paws").assertExists()
        onNodeWithContentDescription("Strokes per day, rising").assertExists()
        table = true
        onNodeWithText("Day 2").assertExists()
        onNodeWithText("21").assertExists()
    }

    @Test
    fun statShowsValueAndDelta() = runComposeUiTest {
        setContent {
            StretchTheme(StretchThemes.WorkshopLight) {
                Stat("Parts drawn", compactNumber(1284.0), delta = Delta("+12% vs last week", good = true), trend = listOf(1f, 3f, 2f, 5f))
            }
        }
        onNodeWithText("1,284").assertExists()
        onNodeWithText("+12% vs last week").assertExists()
        onNodeWithContentDescription("Parts drawn").assertExists()
    }
}
