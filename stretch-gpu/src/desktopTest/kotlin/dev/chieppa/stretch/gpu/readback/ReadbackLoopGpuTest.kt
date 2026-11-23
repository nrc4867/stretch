package dev.chieppa.stretch.gpu.readback

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.chieppa.stretch.frame.SurfacePickQueue
import dev.chieppa.stretch.frame.SurfaceSize
import dev.chieppa.stretch.gpu.GpuFrame
import dev.chieppa.stretch.gpu.GpuOptions
import dev.chieppa.stretch.gpu.GpuPick
import dev.chieppa.stretch.gpu.GpuScene
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.skia.Bitmap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Bounds the waits for frames, so a broken loop fails the test rather than hanging it. */
private const val TIMEOUT_MILLIS = 30_000L

/** A scene that clears the frame to the color it is given. */
private class Cleared : GpuScene<Color> {
    override fun render(frame: GpuFrame, state: Color): Boolean {
        frame.beginPass(state).end()
        return false
    }
}

/** Reads frames back through the loop on a real adapter; runs only with -Pgpu. */
class ReadbackLoopGpuTest {
    private fun ReadbackFrame.pixels(): List<Int> {
        val bitmap = Bitmap.makeFromImage(image)
        val all = (0 until image.height).flatMap { y -> (0 until image.width).map { x -> bitmap.getColor(x, y) } }
        bitmap.close()
        return all
    }

    @Test
    fun deliversEachStateAtAnOddWidthAndAnswersPicks() = runBlocking {
        withTimeout(TIMEOUT_MILLIS) { deliversAndPicks() }
    }

    private suspend fun deliversAndPicks() {
        val delivered = Channel<Unit>(Channel.UNLIMITED)
        val failed = CompletableDeferred<Throwable>()
        val errors = mutableListOf<String>()
        val picks = SurfacePickQueue<GpuPick>()
        val loop = ReadbackLoop(
            Color.Red,
            GpuOptions(sampleCount = 1, onError = { synchronized(errors) { errors += it } }),
            picks,
            onReady = {},
            onFailure = { failed.complete(it) },
            onDelivered = { delivered.trySend(Unit) },
        ) { Cleared() }
        try {
            loop.start()
            // 333 pixels a row leaves padding in every row but the last.
            loop.resize(SurfaceSize(333, 3, 1f))
            delivered.receive()
            val red = checkNotNull(loop.frames.latest.value)
            assertEquals(333 to 3, red.image.width to red.image.height)
            assertTrue(red.pixels().all { it == Color.Red.toArgb() })

            loop.frames.drawn(red)
            loop.publish(Color.Blue)
            var blue = checkNotNull(loop.frames.latest.value)
            while (blue.pixels().first() != Color.Blue.toArgb()) {
                loop.frames.drawn(blue)
                delivered.receive()
                blue = checkNotNull(loop.frames.latest.value)
            }
            assertTrue(blue.pixels().all { it == Color.Blue.toArgb() })

            // The scene draws no IDs, so a pick on it finds nothing; one off it finds nothing without asking.
            assertNull(picks.pick(Offset(10f, 1f)))
            assertNull(picks.pick(Offset(400f, 1f)))
        } finally {
            loop.close()
        }
        assertNull(failed.takeIf { it.isCompleted }?.await(), "The loop failed")
        assertEquals(emptyList(), errors)
    }
}
