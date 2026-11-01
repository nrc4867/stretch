package dev.chieppa.stretch.devtools

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.awt.Dialog
import java.awt.EventQueue
import java.awt.Frame
import java.awt.RenderingHints
import java.awt.Window
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URLDecoder
import javax.imageio.ImageIO

/**
 * A local HTTP endpoint that screenshots the app's own windows, for development tools and agents. Off unless the
 * `stretch.devtools` system property names a port; it only listens on 127.0.0.1.
 *
 * - `GET /windows`: one line per showing window, `index<TAB>title<TAB>width x height`.
 * - `GET /capture?window=<index or part of the title>&scale=<0.1..1>`: that window as PNG, frame and popups included.
 *   Defaults to the first window at full device resolution.
 */
object DevTools {
    const val PROPERTY = "stretch.devtools"

    private var server: HttpServer? = null

    /** Starts the endpoint if the [PROPERTY] system property is set. Safe to call more than once. */
    fun startIfRequested() {
        val port = System.getProperty(PROPERTY)?.toIntOrNull() ?: return
        start(port)
    }

    /** Starts listening on [port] (0 picks a free one) and returns the port in use. */
    fun start(port: Int): Int {
        server?.let { return it.address.port }
        val http = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0)
        http.createContext("/windows") { exchange ->
            exchange.answer { respondText(windows().withIndex().joinToString("") { (i, w) -> "$i\t${w.title()}\t${w.width} x ${w.height}\n" }) }
        }
        http.createContext("/capture") { exchange -> exchange.answer { capture() } }
        http.start()
        server = http
        System.err.println("Stretch devtools: http://127.0.0.1:${http.address.port}")
        return http.address.port
    }

    fun stop() {
        server?.stop(0)
        server = null
    }
}

private fun windows(): List<Window> = onEdt {
    Window.getWindows().filter { it.isShowing && (it is Frame || it is Dialog) }
}

private fun Window.title() = when (this) {
    is Frame -> title.orEmpty()
    is Dialog -> title.orEmpty()
    else -> ""
}

/** Runs [handler], answering 500 with the error rather than leaving the request hanging if it throws. */
private fun HttpExchange.answer(handler: HttpExchange.() -> Unit) {
    try {
        handler()
    } catch (e: Exception) {
        runCatching { respondText(e.stackTraceToString(), 500) }
    } finally {
        close()
    }
}

private fun HttpExchange.capture() {
    if (!captureSupported) return respondText("Capture is only implemented on Windows", 501)
    val query = query()
    val all = windows()
    val pick = query["window"]
    val window = when {
        pick == null -> all.firstOrNull()
        pick.toIntOrNull() != null -> all.getOrNull(pick.toInt())
        else -> all.firstOrNull { it.title().contains(pick, ignoreCase = true) }
    } ?: return respondText("No showing window matches '${pick.orEmpty()}'", 404)
    if (window is Frame && window.extendedState and Frame.ICONIFIED != 0) return respondText("Window is minimized", 409)
    val scale = (query["scale"]?.toDoubleOrNull() ?: 1.0).coerceIn(0.1, 1.0)
    val png = ByteArrayOutputStream().also { ImageIO.write(capture(window).scaled(scale), "png", it) }.toByteArray()
    responseHeaders.add("Content-Type", "image/png")
    sendResponseHeaders(200, png.size.toLong())
    responseBody.use { it.write(png) }
}

private fun BufferedImage.scaled(scale: Double): BufferedImage {
    if (scale >= 1.0) return this
    val out = BufferedImage((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1), BufferedImage.TYPE_INT_RGB)
    val g = out.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g.drawImage(this, 0, 0, out.width, out.height, null)
    g.dispose()
    return out
}

private fun HttpExchange.query(): Map<String, String> =
    requestURI.rawQuery.orEmpty().split('&').filter { '=' in it }.associate {
        val (k, v) = it.split('=', limit = 2)
        URLDecoder.decode(k, Charsets.UTF_8) to URLDecoder.decode(v, Charsets.UTF_8)
    }

private fun HttpExchange.respondText(text: String, status: Int = 200) {
    val bytes = text.toByteArray()
    responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
    sendResponseHeaders(status, bytes.size.toLong())
    responseBody.use { it.write(bytes) }
}

private fun <T> onEdt(block: () -> T): T {
    if (EventQueue.isDispatchThread()) return block()
    var result: Result<T>? = null
    EventQueue.invokeAndWait { result = runCatching(block) }
    return result!!.getOrThrow()
}
