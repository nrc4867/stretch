package dev.chieppa.stretch.web

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import dev.chieppa.stretch.test.runComposeUiTestAwaiting
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Starts the real engine, so it runs only with -Pcef. It writes what it saw to build/engine-test. Tests wait for the
 * engine through [EngineWork] and the page's own messages.
 */
@OptIn(ExperimentalTestApi::class)
class HtmlViewEngineTest {
    private val output = File(System.getProperty("stretch.web.output")).also { it.mkdirs() }

    private val timeout = 60_000L

    @Test
    fun engineIsThere() {
        assertTrue(WebEngine.available)
    }

    @Test
    fun scriptPageRunsScriptsAndReportsFailures() = runBlocking {
        withTimeout(timeout) {
            val page = ScriptPage()
            page.load("window.twice = (x) => x * 2;")
            assertEquals("42", page.call("twice(21)"))
            assertEquals("7", page.call("new Promise((ok) => setTimeout(() => ok(7), 10))"))
            assertEquals("24", page.call("document.body.offsetWidth >= 0 ? 24 : 0"))
            var message: String? = null
            try {
                page.call("nothingHere()")
            } catch (e: ScriptException) {
                message = e.message
            }
            assertTrue(message.orEmpty().contains("nothingHere"), "got $message")
            page.close()
        }
    }

    @Test
    fun htmlViewDrawsAPageRunningItsScripts() = runComposeUiTestAwaiting(EngineWork) {
        val document = """<!doctype html><body style="margin:0;background:rgb(10,200,30)">
            <h1 id=t>Stretch HTML view</h1>
            <script>document.body.style.background='rgb(200,20,40)'; document.getElementById('t').textContent += ' with scripts';</script>"""
        setContent {
            StretchTheme(StretchThemes.Ink) {
                HtmlView(HtmlContent.Inline(document), Modifier.size(400.dp, 200.dp).testTag("page"))
            }
        }
        waitForIdle()
        val image = onNodeWithTag("page").captureToImage().toAwtImage()
        ImageIO.write(image, "png", File(output, "html-view.png"))
        assertTrue(isRed(image.getRGB(image.width - 10, image.height - 10)), "the script did not run")
    }

    @Test
    fun aPolicyStopsScriptsInADocument() = runComposeUiTestAwaiting(EngineWork) {
        val document = """<body style="margin:0;background:rgb(0,0,200)"><script>document.body.style.background='rgb(200,20,40)'</script>"""
        setContent {
            StretchTheme(StretchThemes.Ink) {
                HtmlView(HtmlContent.Inline(document), Modifier.size(100.dp).testTag("page"), HtmlPolicy(scripts = false))
            }
        }
        waitForIdle()
        val image = onNodeWithTag("page").captureToImage().toAwtImage()
        assertEquals(200, image.getRGB(image.width / 2, image.height / 2) and 0xFFFFFF, "the script ran")
    }

    @Test
    fun aDocumentFollowsTheTheme() = runComposeUiTestAwaiting(EngineWork) {
        var theme by mutableStateOf(StretchThemes.Ink)
        setContent {
            StretchTheme(theme) {
                HtmlView(HtmlContent.Inline("<body>"), Modifier.size(100.dp).testTag("page"))
            }
        }
        waitForIdle()
        val before = onNodeWithTag("page").captureToImage().toAwtImage().let { it.getRGB(it.width / 2, it.height / 2) }
        theme = StretchThemes.WorkshopDark
        waitForIdle()
        val after = onNodeWithTag("page").captureToImage().toAwtImage().let { it.getRGB(it.width / 2, it.height / 2) }
        assertTrue(before != after, "the page kept its colors")
    }

    @Test
    fun aBridgeCarriesMessagesBothWaysAndThePageSeesTheTheme() = runComposeUiTest {
        val heard = LinkedBlockingQueue<String>()
        val bridge = HtmlBridge { heard += it }
        bridge.post("ping")
        val document = """<!doctype html><body><script>
            stretch.listen((m) => stretch.post('echo:' + m));
            stretch.post('canvas:' + getComputedStyle(document.documentElement).getPropertyValue('--stretch-surface-canvas').trim());
            </script>"""
        setContent {
            StretchTheme(StretchThemes.Ink) {
                HtmlView(HtmlContent.Inline(document), Modifier.size(100.dp), bridge = bridge)
            }
        }
        val first = listOf(heard.next(), heard.next())
        assertTrue("echo:ping" in first && first.any { it.startsWith("canvas:#") }, "got $first")
        runOnIdle { bridge.post("pong") }
        assertEquals("echo:pong", heard.next())
    }

    /** The next message, waiting for it as long as the engine may take. */
    private fun LinkedBlockingQueue<String>.next(): String = assertNotNull(poll(timeout, TimeUnit.MILLISECONDS), "no message")

    private fun isRed(rgb: Int): Boolean {
        val r = rgb shr 16 and 255
        val g = rgb shr 8 and 255
        val b = rgb and 255
        return r > 150 && g < 80 && b < 90
    }
}
