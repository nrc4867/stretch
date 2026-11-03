package dev.chieppa.stretch.devtools

import java.awt.Color
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DevToolsTest {
    private val client = HttpClient.newHttpClient()
    private var frame: JFrame? = null

    @AfterTest
    fun tearDown() {
        DevTools.stop()
        frame?.let { EventQueue.invokeAndWait { it.dispose() } }
    }

    private fun get(port: Int, path: String) =
        client.send(HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path")).build(), HttpResponse.BodyHandlers.ofByteArray())

    @Test
    fun capturesOnlyTheNamedWindow() {
        if (!captureSupported || GraphicsEnvironment.isHeadless()) return
        EventQueue.invokeAndWait {
            frame = JFrame("Capture probe").apply {
                contentPane = JPanel().apply { background = Color(0xD0, 0x20, 0x30) }
                setSize(320, 240)
                setLocation(80, 80)
                isVisible = true
            }
        }
        Thread.sleep(500)
        val port = DevTools.start(0)

        val list = String(get(port, "/windows").body())
        assertTrue("Capture probe" in list, list)

        val response = get(port, "/capture?window=probe")
        assertEquals(200, response.statusCode())
        val image = ImageIO.read(response.body().inputStream())
        // The content pane, not the 320 x 240 given to setSize: that counts the invisible resize border, which capture leaves out.
        val content = frame!!.contentPane.size
        assertTrue(image.width >= content.width && image.height >= content.height, "${image.width} x ${image.height}, content $content")
        val center = Color(image.getRGB(image.width / 2, image.height / 2))
        assertEquals(Color(0xD0, 0x20, 0x30), center)

        assertEquals(404, get(port, "/capture?window=no-such-window").statusCode())
    }
}
