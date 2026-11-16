package dev.chieppa.stretch.gpu

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A scene that clears the frame to the color it is given. */
private class Cleared : GpuScene<Color> {
    var closed = false

    override fun render(frame: GpuFrame, state: Color): Boolean {
        frame.beginPass(state).end()
        return false
    }

    override fun close() {
        closed = true
    }
}

/** Renders several images from one device on a real adapter; runs only with -Pgpu. */
class GpuSnapshotsGpuTest {
    @Test
    fun rendersEachStateAtItsOwnSizeFromOneSceneAndClosesItOnce() = runTest {
        val errors = mutableListOf<String>()
        var made = 0
        val scene = Cleared()
        GpuSnapshots.open(GpuOptions(sampleCount = 1, onError = { synchronized(errors) { errors += it } })) {
            made++
            scene
        }.use { snapshots ->
            val red = snapshots.render(Color.Red, 4, 2)
            val blue = snapshots.render(Color.Blue, 3, 5)
            val again = snapshots.render(Color.Red, 3, 5)
            assertEquals(listOf(4 to 2, 3 to 5), listOf(red.width to red.height, blue.width to blue.height))
            assertTrue(red.pixels.all { it == Color.Red.toArgb() })
            assertTrue(blue.pixels.all { it == Color.Blue.toArgb() })
            assertTrue(again.pixels.all { it == Color.Red.toArgb() })
        }
        assertEquals(1, made)
        assertTrue(scene.closed)
        assertEquals(emptyList(), errors)
    }
}
