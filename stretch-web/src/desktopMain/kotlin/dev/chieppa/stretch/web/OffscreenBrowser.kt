package dev.chieppa.stretch.web

import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.browser.CefRendering
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefDragData
import org.cef.callback.CefJSDialogCallback
import org.cef.callback.CefMenuModel
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefContextMenuHandlerAdapter
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefJSDialogHandler
import org.cef.handler.CefJSDialogHandlerAdapter
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.cef.handler.CefRenderHandler
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefScreenInfo
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.awt.Canvas
import java.awt.Point
import java.awt.Rectangle
import java.nio.ByteBuffer

private const val BYTES_PER_PIXEL = 4
private const val SCREEN_DEPTH = 32
private const val DEPTH_PER_COMPONENT = 8

/** What a browser reports back. Callbacks arrive on Chromium's thread. */
internal interface BrowserEvents {
    /** A new picture of the page, or of the popup it has open such as a `<select>` list. The receiver owns the image. */
    fun onFrame(image: Image, popup: Boolean) = image.close()
    fun onPopup(bounds: Rectangle?) = Unit
    fun onCursor(type: Int) = Unit
    fun onLoaded() = Unit

    /** A page wants to go to [url]. Return true to allow it. */
    fun onNavigate(url: String): Boolean = true
    fun onMessage(message: String) = Unit
}

/**
 * A Chromium browser that draws to memory instead of a window, so its pictures can be drawn by Compose on every OS. Its
 * size is in points; [scale] is how many pixels each point has. Create and use it on the event thread.
 */
internal class OffscreenBrowser(url: String, width: Int, height: Int, scale: Float, private val events: BrowserEvents) {
    @Volatile private var viewWidth = width.coerceAtLeast(1)
    @Volatile private var viewHeight = height.coerceAtLeast(1)
    @Volatile private var scale = scale.toDouble()

    /** The address [load] last went to, which goes ahead without asking [BrowserEvents.onNavigate]. */
    @Volatile private var loading: String? = null

    private val client: CefClient = CefRuntime.app.createClient()
    private val router: CefMessageRouter = CefMessageRouter.create(
        CefMessageRouter.CefMessageRouterConfig(QUERY_FUNCTION, CANCEL_FUNCTION),
        object : CefMessageRouterHandlerAdapter() {
            override fun onQuery(browser: CefBrowser, frame: CefFrame, id: Long, request: String, persistent: Boolean, callback: CefQueryCallback): Boolean {
                events.onMessage(request)
                callback.success("")
                return true
            }
        },
    )
    val browser: CefBrowser

    init {
        client.addMessageRouter(router)
        client.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            override fun onBeforePopup(browser: CefBrowser, frame: CefFrame, target: String?, name: String?): Boolean {
                if (target != null) events.onNavigate(target)
                return true
            }
        })
        client.addRequestHandler(object : CefRequestHandlerAdapter() {
            override fun onBeforeBrowse(browser: CefBrowser, frame: CefFrame, request: CefRequest, userGesture: Boolean, isRedirect: Boolean): Boolean {
                val target = request.url ?: return false
                val current = browser.url.orEmpty()
                if (!frame.isMain || !browser.hasDocument() || isSamePage(current, target) || target == loading) return false
                return !events.onNavigate(target)
            }
        })
        client.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, status: Int) {
                if (frame.isMain) events.onLoaded()
            }
        })
        client.addDisplayHandler(object : CefDisplayHandlerAdapter() {
            override fun onConsoleMessage(browser: CefBrowser, level: org.cef.CefSettings.LogSeverity, message: String, source: String, line: Int) = true
        })
        client.addContextMenuHandler(object : CefContextMenuHandlerAdapter() {
            override fun onBeforeContextMenu(browser: CefBrowser, frame: CefFrame, params: CefContextMenuParams, model: CefMenuModel) {
                model.clear()
            }
        })
        client.addJSDialogHandler(object : CefJSDialogHandlerAdapter() {
            override fun onJSDialog(
                browser: CefBrowser,
                origin: String?,
                type: CefJSDialogHandler.JSDialogType?,
                text: String?,
                prompt: String?,
                callback: CefJSDialogCallback?,
                suppress: BoolRef,
            ): Boolean {
                suppress.set(true)
                return false
            }
        })
        val rendering = CefRendering.CefRenderingWithHandler(Renderer(), PLACEHOLDER)
        browser = client.createBrowser(url, rendering, true)
        browser.createImmediately()
    }

    /** Goes to [url] in place of the page shown, as the app's own choice rather than the page's. */
    fun load(url: String) {
        loading = url
        browser.loadURL(url)
    }

    fun resize(width: Int, height: Int, scale: Float) {
        val scaleChanged = this.scale != scale.toDouble()
        viewWidth = width.coerceAtLeast(1)
        viewHeight = height.coerceAtLeast(1)
        this.scale = scale.toDouble()
        if (scaleChanged) browser.notifyScreenInfoChanged()
        browser.wasResized(viewWidth, viewHeight)
    }

    fun close() {
        browser.setCloseAllowed()
        browser.close(true)
        client.removeMessageRouter(router)
        router.dispose()
        client.dispose()
    }

    private inner class Renderer : CefRenderHandler {
        private var pixels = ByteArray(0)

        override fun getViewRect(browser: CefBrowser) = Rectangle(0, 0, viewWidth, viewHeight)

        override fun getScreenPoint(browser: CefBrowser, view: Point) = view

        override fun getDeviceScaleFactor(browser: CefBrowser) = scale

        override fun getScreenInfo(browser: CefBrowser, info: CefScreenInfo): Boolean {
            val area = Rectangle(0, 0, viewWidth, viewHeight)
            info.Set(scale, SCREEN_DEPTH, DEPTH_PER_COMPONENT, false, area, area)
            return true
        }

        override fun onPopupShow(browser: CefBrowser, show: Boolean) {
            if (!show) events.onPopup(null)
        }

        override fun onPopupSize(browser: CefBrowser, size: Rectangle) = events.onPopup(size)

        override fun onPaint(browser: CefBrowser, popup: Boolean, dirty: Array<out Rectangle>, buffer: ByteBuffer, width: Int, height: Int) {
            val size = width * height * BYTES_PER_PIXEL
            if (pixels.size != size) pixels = ByteArray(size)
            buffer.position(0)
            buffer.get(pixels, 0, size)
            val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.PREMUL)
            events.onFrame(Image.makeRaster(info, pixels, width * BYTES_PER_PIXEL), popup)
        }

        override fun onCursorChange(browser: CefBrowser, cursorType: Int): Boolean {
            events.onCursor(cursorType)
            return true
        }

        override fun startDragging(browser: CefBrowser, data: CefDragData, mask: Int, x: Int, y: Int) = false

        override fun updateDragCursor(browser: CefBrowser, operation: Int) = Unit
    }

    companion object {
        const val QUERY_FUNCTION = "stretchQuery"
        const val CANCEL_FUNCTION = "stretchQueryCancel"

        /** JCEF wants a component to size against; [Renderer] answers for size itself, so this one is never shown. */
        private val PLACEHOLDER by lazy { Canvas() }
    }
}
