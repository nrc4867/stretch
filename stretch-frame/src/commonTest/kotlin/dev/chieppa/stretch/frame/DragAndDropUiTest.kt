package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.ui.focusHome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DragAndDropUiTest {
    private class Log {
        val hovers = mutableListOf<Pair<String?, Offset>>()
        val drops = mutableListOf<Pair<String, Offset>>()
    }

    private fun ComposeUiTest.show(
        item: String?,
        log: Log,
        accepts: (String) -> Boolean = { true },
        take: Boolean = true,
        host: Boolean = true,
        numbers: MutableList<Int> = mutableListOf(),
    ) {
        val scene = @androidx.compose.runtime.Composable {
            Row(Modifier.fillMaxSize().focusHome().testTag("home")) {
                Box(Modifier.size(60.dp).testTag("source").dragSource({ item }) { BasicText("Lifted $item") })
                Spacer(Modifier.width(100.dp))
                Box(
                    Modifier.size(200.dp).testTag("target")
                        .dropTarget<Int>(accepts = { true }, onDrop = { n, _ -> numbers += n; true })
                        .dropTarget<String>(
                            accepts = accepts,
                            onHover = { dragged, at -> log.hovers += dragged to at },
                            onDrop = { dragged, at -> log.drops += dragged to at; take },
                        ),
                )
            }
        }
        setStretchContent { if (host) DragAndDropHost { scene() } else scene() }
    }

    /** Presses on the source's middle and moves to [to], in pixels from the root's top left, without letting go. */
    private fun ComposeUiTest.dragTo(to: Offset) {
        val source = onNodeWithTag("source").getUnclippedBoundsInRoot()
        val start = with(density) { Offset(((source.left + source.right) / 2).toPx(), ((source.top + source.bottom) / 2).toPx()) }
        onRoot().performMouseInput {
            moveTo(start)
            press()
            moveTo(start + Offset(20f, 0f))
            moveTo(start + Offset(60f, 0f))
            moveTo(to)
        }
    }

    private fun ComposeUiTest.targetPoint(x: Float, y: Float): Offset {
        val target = onNodeWithTag("target").getUnclippedBoundsInRoot()
        return with(density) { Offset(target.left.toPx() + x, target.top.toPx() + y) }
    }

    @Test
    fun aDragDropsTheItemOnTheTargetWhereThePointerLetsGo() = runComposeUiTest {
        val log = Log()
        show("leg", log)
        dragTo(targetPoint(30f, 40f))
        onNodeWithText("Lifted leg").assertExists()
        assertEquals("leg" to Offset(30f, 40f), log.hovers.last())
        onRoot().performMouseInput { release() }
        waitForIdle()
        assertEquals(listOf("leg" to Offset(30f, 40f)), log.drops)
        assertNull(log.hovers.last().first)
        onNodeWithText("Lifted leg").assertDoesNotExist()
    }

    @Test
    fun thePreviewSeesTheLocalsAroundItsSource() = runComposeUiTest {
        val word = staticCompositionLocalOf { "outside" }
        setStretchContent {
            DragAndDropHost {
                CompositionLocalProvider(word provides "inside") {
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.size(60.dp).testTag("source").dragSource({ "leg" }) { BasicText("Lifted ${word.current}") })
                        Spacer(Modifier.width(100.dp))
                        Box(Modifier.size(200.dp).testTag("target"))
                    }
                }
            }
        }
        dragTo(targetPoint(30f, 40f))
        onNodeWithText("Lifted inside").assertExists()
        onRoot().performMouseInput { release() }
    }

    @Test
    fun aPreviewThatFillsItsSpaceComesOutTheSourcesSize() = runComposeUiTest {
        setStretchContent {
            DragAndDropHost {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.size(60.dp, 40.dp).testTag("source").dragSource({ "leg" }) { Box(Modifier.fillMaxSize().testTag("preview")) })
                    Spacer(Modifier.width(100.dp))
                    Box(Modifier.size(200.dp).testTag("target"))
                }
            }
        }
        dragTo(targetPoint(30f, 40f))
        val preview = onNodeWithTag("preview").getUnclippedBoundsInRoot()
        assertEquals(60.dp, preview.right - preview.left)
        assertEquals(40.dp, preview.bottom - preview.top)
        onRoot().performMouseInput { release() }
    }

    @Test
    fun aPreviewSmallerThanItsSourceKeepsItsOwnSize() = runComposeUiTest {
        setStretchContent {
            DragAndDropHost {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.size(60.dp, 40.dp).testTag("source").dragSource({ "leg" }) { Box(Modifier.size(20.dp, 10.dp).testTag("preview")) })
                    Spacer(Modifier.width(100.dp))
                    Box(Modifier.size(200.dp).testTag("target"))
                }
            }
        }
        dragTo(targetPoint(30f, 40f))
        val preview = onNodeWithTag("preview").getUnclippedBoundsInRoot()
        assertEquals(20.dp, preview.right - preview.left)
        assertEquals(10.dp, preview.bottom - preview.top)
        onRoot().performMouseInput { release() }
    }

    @Test
    fun escCancelsTheDrag() = runComposeUiTest {
        val log = Log()
        show("leg", log)
        dragTo(targetPoint(30f, 40f))
        onNodeWithTag("home").performKeyInput { pressKey(Key.Escape) }
        assertNull(log.hovers.last().first)
        onRoot().performMouseInput { release() }
        waitForIdle()
        assertTrue(log.drops.isEmpty())
        onNodeWithText("Lifted leg").assertDoesNotExist()
    }

    @Test
    fun aTargetThatRefusesTheItemNeverHearsIt() = runComposeUiTest {
        val log = Log()
        show("leg", log, accepts = { it != "leg" })
        dragTo(targetPoint(30f, 40f))
        onRoot().performMouseInput { release() }
        waitForIdle()
        assertTrue(log.hovers.isEmpty())
        assertTrue(log.drops.isEmpty())
    }

    @Test
    fun aDropNothingTakesGoesBackAndEnds() = runComposeUiTest {
        val log = Log()
        show("leg", log, take = false)
        dragTo(targetPoint(30f, 40f))
        onRoot().performMouseInput { release() }
        waitForIdle()
        assertEquals(1, log.drops.size)
        onNodeWithText("Lifted leg").assertDoesNotExist()
    }

    @Test
    fun itemsOfAnotherTypePassOverATarget() = runComposeUiTest {
        val log = Log()
        val numbers = mutableListOf<Int>()
        show("leg", log, numbers = numbers)
        dragTo(targetPoint(30f, 40f))
        onRoot().performMouseInput { release() }
        waitForIdle()
        assertEquals(1, log.drops.size)
        assertTrue(numbers.isEmpty())
    }

    @Test
    fun noItemMeansNoDrag() = runComposeUiTest {
        val log = Log()
        show(null, log)
        dragTo(targetPoint(30f, 40f))
        onRoot().performMouseInput { release() }
        waitForIdle()
        assertTrue(log.hovers.isEmpty())
        assertTrue(log.drops.isEmpty())
    }

    @Test
    fun withoutAHostASourceDoesNothing() = runComposeUiTest {
        val log = Log()
        show("leg", log, host = false)
        dragTo(targetPoint(30f, 40f))
        onRoot().performMouseInput { release() }
        waitForIdle()
        assertTrue(log.drops.isEmpty())
    }

    /** A source dragging "leg" with a 20 dp preview, and a target that snaps the preview to [snapAt] while the pointer is left of its middle. */
    private fun ComposeUiTest.showSnapping(snapAt: Offset, log: Log, seen: (DragAndDrop?) -> Unit = {}) {
        setStretchContent {
            DragAndDropHost {
                seen(LocalDragAndDrop.current)
                Row(Modifier.fillMaxSize().focusHome().testTag("home")) {
                    Box(Modifier.size(60.dp).testTag("source").dragSource({ "leg" }) { Box(Modifier.size(20.dp).testTag("preview")) })
                    Spacer(Modifier.width(100.dp))
                    Box(
                        Modifier.size(200.dp).testTag("target").dropTarget<String>(
                            accepts = { true },
                            onHover = { dragged, at -> log.hovers += dragged to at },
                            snap = { _, at -> if (at.x < 100f) DropSnap(snapAt) else null },
                            onDrop = { dragged, at -> log.drops += dragged to at; true },
                        ),
                    )
                }
            }
        }
    }

    private fun ComposeUiTest.previewCentre(): Offset {
        val preview = onNodeWithTag("preview").getUnclippedBoundsInRoot()
        return with(density) { Offset(((preview.left + preview.right) / 2).toPx(), ((preview.top + preview.bottom) / 2).toPx()) }
    }

    @Test
    fun theItemShowsWhileThePointerHoldsItAndClearsOnTheDrop() = runComposeUiTest {
        var drags: DragAndDrop? = null
        showSnapping(Offset(10f, 10f), Log()) { drags = it }
        assertNull(drags?.item)
        dragTo(targetPoint(150f, 40f))
        assertEquals("leg", drags?.item)
        onRoot().performMouseInput { release() }
        waitForIdle()
        assertNull(drags?.item)
    }

    @Test
    fun theItemClearsAsSoonAsEscSendsItBack() = runComposeUiTest {
        var drags: DragAndDrop? = null
        showSnapping(Offset(10f, 10f), Log()) { drags = it }
        dragTo(targetPoint(150f, 40f))
        mainClock.autoAdvance = false
        onNodeWithTag("home").performKeyInput { pressKey(Key.Escape) }
        assertNull(drags?.item)
        onNodeWithTag("preview").assertExists()
        onRoot().performMouseInput { release() }
    }

    @Test
    fun aTargetSnapsThePreviewOntoItsPointWhileThePointerIsNear() = runComposeUiTest {
        val log = Log()
        showSnapping(Offset(80f, 90f), log)
        dragTo(targetPoint(30f, 40f))
        val centre = previewCentre()
        val expected = targetPoint(80f, 90f)
        assertEquals(expected.x, centre.x, 1f)
        assertEquals(expected.y, centre.y, 1f)
        assertEquals("leg" to Offset(30f, 40f), log.hovers.last())
        onRoot().performMouseInput { release() }
    }

    @Test
    fun movingAwayFromTheSnapLetsThePreviewFollowThePointerAgain() = runComposeUiTest {
        showSnapping(Offset(80f, 90f), Log())
        dragTo(targetPoint(30f, 40f))
        onRoot().performMouseInput { moveTo(targetPoint(150f, 40f)) }
        val centre = previewCentre()
        val pointer = targetPoint(150f, 40f)
        assertEquals(pointer.x, centre.x, 1f)
        assertEquals(pointer.y, centre.y, 1f)
        onRoot().performMouseInput { release() }
    }

    @Test
    fun aDropWhileSnappedGetsThePointer() = runComposeUiTest {
        val log = Log()
        showSnapping(Offset(80f, 90f), log)
        dragTo(targetPoint(30f, 40f))
        onRoot().performMouseInput { release() }
        waitForIdle()
        assertEquals(listOf("leg" to Offset(30f, 40f)), log.drops)
        onNodeWithTag("preview").assertDoesNotExist()
    }
}
