package dev.chieppa.stretch.devtools

import java.awt.image.BufferedImage
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The endpoint with no window showing. [DevToolsScreenTest] captures a real one. */
class DevToolsTest {
    private val client = HttpClient.newHttpClient()

    @AfterTest
    fun tearDown() {
        DevTools.stop()
    }

    private fun get(port: Int, path: String) =
        client.send(HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path")).build(), HttpResponse.BodyHandlers.ofString())

    @Test
    fun listsNoWindowsWhenNoneIsShowing() {
        val port = DevTools.start(0)
        val list = get(port, "/windows")
        assertEquals(200, list.statusCode())
        assertEquals("", list.body())
        if (captureSupported) assertEquals(404, get(port, "/capture?window=no-such-window").statusCode())
    }

    @Test
    fun aPopupsSeeThroughPixelsStaySeeThrough() {
        val popup = BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB)
        popup.setRGB(1, 0, 0x1A1416)
        val shown = seeThrough(popup)
        assertEquals(0, shown.getRGB(0, 0) ushr 24)
        assertEquals(0xFF1A1416.toInt(), shown.getRGB(1, 0))
    }
}
