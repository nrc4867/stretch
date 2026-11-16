package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
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
}
