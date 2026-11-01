package dev.chieppa.stretch.frame

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SurfaceLoopTest {
    private val handle = SurfaceHandle.Win32(1, 2)
    private val size = SurfaceSize(640, 480, 1.5f)

    /** Records every call; [render] returns true while [animateFrames] frames remain. */
    private class Recorder(var animateFrames: Int = 0) : SurfaceRenderer<String> {
        val calls = CopyOnWriteArrayList<String>()
        val rendered = CopyOnWriteArrayList<Pair<String, SurfaceSize>>()
        val detached = CountDownLatch(1)
        @Volatile var frames = CountDownLatch(1)

        override fun attach(handle: SurfaceHandle) {
            calls += "attach"
        }

        override fun render(state: String, size: SurfaceSize): Boolean {
            calls += "render"
            rendered += state to size
            frames.countDown()
            return animateFrames-- > 0
        }

        override fun detach() {
            calls += "detach"
            detached.countDown()
        }
    }

    private fun CountDownLatch.awaitOrFail() = assertTrue(await(5, TimeUnit.SECONDS), "timed out")

    @Test
    fun rendersTheLatestStateOnceThereIsASize() {
        val renderer = Recorder()
        val loop = SurfaceLoop(handle, renderer, "first", onFailure = { throw it })
        loop.resize(size)
        renderer.frames.awaitOrFail()
        assertEquals("first" to size, renderer.rendered.last())

        renderer.frames = CountDownLatch(1)
        loop.publish("second")
        renderer.frames.awaitOrFail()
        assertEquals("second" to size, renderer.rendered.last())

        loop.close()
        renderer.detached.awaitOrFail()
        assertEquals("attach", renderer.calls.first())
        assertEquals("detach", renderer.calls.last())
    }

    @Test
    fun keepsRenderingWhileTheRendererAnimates() {
        val renderer = Recorder(animateFrames = 3)
        renderer.frames = CountDownLatch(4)
        val loop = SurfaceLoop(handle, renderer, "spin", onFailure = { throw it })
        loop.resize(size)
        renderer.frames.awaitOrFail()
        loop.close()
        assertEquals(4, renderer.rendered.size)
    }

    @Test
    fun neverRendersAnEmptySize() {
        val renderer = Recorder()
        val loop = SurfaceLoop(handle, renderer, "hidden", onFailure = { throw it })
        loop.resize(SurfaceSize(0, 300, 1f))
        loop.publish("still hidden")
        loop.resize(size)
        renderer.frames.awaitOrFail()
        loop.close()
        assertTrue(renderer.rendered.none { it.second.isEmpty })
    }

    @Test
    fun reportsWhatTheRendererThrowsAndDetaches() {
        val boom = IllegalStateException("no adapter")
        val reported = CountDownLatch(1)
        var failure: Throwable? = null
        val renderer = object : SurfaceRenderer<Unit> {
            var detached = false
            override fun attach(handle: SurfaceHandle) = Unit
            override fun render(state: Unit, size: SurfaceSize): Boolean = throw boom
            override fun detach() {
                detached = true
            }
        }
        val loop = SurfaceLoop(handle, renderer, Unit, onFailure = { failure = it; reported.countDown() })
        loop.resize(size)
        reported.awaitOrFail()
        loop.close()
        assertSame(boom, failure)
        assertTrue(renderer.detached)
    }
}
