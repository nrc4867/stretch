package dev.chieppa.stretch.devtools

import java.awt.Color
import java.awt.EventQueue
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.imageio.ImageIO
import javax.swing.JDialog
import javax.swing.JPanel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Captures a real window. It runs only with -Pscreen. The window is on screen, since the OS keeps no picture of one off
 * every screen, but wholly see-through and without a taskbar button or focus, so no one sees it.
 */
class DevToolsScreenTest {
    private val client = HttpClient.newHttpClient()
    private var window: JDialog? = null

    @AfterTest
    fun tearDown() {
        DevTools.stop()
        window?.let { EventQueue.invokeAndWait { it.dispose() } }
    }

    private fun get(port: Int, path: String) =
        client.send(HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path")).build(), HttpResponse.BodyHandlers.ofByteArray())

    @Test
    fun capturesOnlyTheNamedWindow() {
        if (!captureSupported || GraphicsEnvironment.isHeadless()) return
        EventQueue.invokeAndWait {
            window = JDialog(null as Frame?, "Capture probe").apply {
                isUndecorated = true
                opacity = 0f
                focusableWindowState = false
                contentPane = JPanel().apply { background = PROBE }
                setSize(320, 240)
                setLocation(80, 80)
                isVisible = true
            }
        }
        // A see-through window is never asked to paint, so it paints now, on the spot.
        EventQueue.invokeAndWait { window!!.rootPane.run { paintImmediately(0, 0, width, height) } }
        val port = DevTools.start(0)

        val list = String(get(port, "/windows").body())
        assertTrue("Capture probe" in list, list)

        val response = get(port, "/capture?window=probe")
        assertEquals(200, response.statusCode())
        val image = ImageIO.read(response.body().inputStream())
        val content = window!!.contentPane.size
        assertTrue(image.width >= content.width && image.height >= content.height, "${image.width} x ${image.height}, content $content")
        assertEquals(PROBE, Color(image.getRGB(image.width / 2, image.height / 2)))

        assertEquals(404, get(port, "/capture?window=no-such-window").statusCode())
    }

    private companion object {
        val PROBE = Color(0xD0, 0x20, 0x30)
    }
}
