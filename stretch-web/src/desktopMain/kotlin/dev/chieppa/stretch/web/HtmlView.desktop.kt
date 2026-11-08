package dev.chieppa.stretch.web

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import dev.chieppa.stretch.core.AppLaunch
import dev.chieppa.stretch.core.Requirement
import dev.chieppa.stretch.theme.Stretch
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import java.awt.Cursor
import java.awt.Rectangle
import java.util.Base64
import javax.swing.SwingUtilities
import kotlin.math.ceil

private const val ENGINE_CLASS = "org.cef.CefApp"
private const val DISABLED_PROPERTY = "stretch.web.disabled"

actual object WebEngine {
    actual val available: Boolean by lazy {
        System.getProperty(DISABLED_PROPERTY) == null && runCatching { Class.forName(ENGINE_CLASS, false, javaClass.classLoader) }.isSuccess
    }

    actual val requirement: Requirement get() = engineRequirement

    /**
     * Call first in `main`, with its arguments. Where this runtime has no engine but one was installed through
     * [requirement], it runs the app on that runtime instead, then exits with it; otherwise it returns at once.
     */
    fun startOnInstalledRuntime(args: Array<String>) {
        AppLaunch.remember(args)
        if (available || AppLaunch.restarted) return
        AppLaunch.startAgain(engineRequirement.installedHome() ?: return, waitAndExit = true)
    }
}

/**
 * A browser's latest pictures, swapped on the event thread so drawing never sees a closed image. From when a page starts
 * loading until the first picture after it has loaded, the view counts as [EngineWork]. Used on the event thread.
 */
private class PageFrames {
    var page by mutableStateOf<Image?>(null)
    var popup by mutableStateOf<Image?>(null)
    var popupBounds by mutableStateOf<Rectangle?>(null)
    var cursor by mutableStateOf(Cursor.DEFAULT_CURSOR)

    private var waiting = false
    private var hasLoaded = false

    /** Notes that a page starts loading, so the view waits for its picture. */
    fun loading() {
        if (!waiting) EngineWork.begin()
        waiting = true
        hasLoaded = false
    }

    /** Notes that the page has loaded, returning whether the view waits for a picture of it. */
    fun loaded(): Boolean {
        hasLoaded = waiting
        return waiting
    }

    private fun shown() {
        if (waiting) EngineWork.end()
        waiting = false
        hasLoaded = false
    }

    fun show(image: Image, isPopup: Boolean) = SwingUtilities.invokeLater {
        if (isPopup) {
            popup?.close()
            popup = image
        } else {
            page?.close()
            page = image
            if (hasLoaded) shown()
        }
    }

    fun clear() {
        page?.close()
        popup?.close()
        page = null
        popup = null
        shown()
    }
}

/** The page's side of an [HtmlBridge]: messages go out through JCEF's message router. */
private val desktopBridgeScript = bridgeScript("window.${OffscreenBrowser.QUERY_FUNCTION}({request:m})")

/** What to load for [content]: an address, with inline documents carried in the address itself. */
private fun addressOf(content: HtmlContent, policy: HtmlPolicy, look: HtmlLook, bridged: Boolean): String = when (content) {
    is HtmlContent.Url -> content.url
    is HtmlContent.Inline -> {
        val document = prepareDocument(content.html, policy, content.baseUrl, look, if (bridged) desktopBridgeScript else null)
        "data:text/html;charset=utf-8;base64," + Base64.getEncoder().encodeToString(document.toByteArray())
    }
}

/** Runs [block] on the event thread, where JCEF wants its calls. Compose is already there in a window. */
private fun onEventThread(block: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeAndWait(block)
}

/** Points the page is laid out in for a box of [pixels]: Compose's density matches the page's, interface size included. */
private fun pointsOf(pixels: Int, density: Density) = ceil(pixels / density.density).toInt()

@Composable
actual fun HtmlView(
    content: HtmlContent,
    modifier: Modifier,
    policy: HtmlPolicy,
    navigation: HtmlNavigation,
    bridge: HtmlBridge?,
    unavailable: @Composable () -> Unit,
) {
    if (!WebEngine.available) {
        unavailable()
        return
    }
    val density = LocalDensity.current
    val look = currentHtmlLook
    val frames = remember { PageFrames() }
    val currentNavigation by rememberUpdatedState(navigation)
    val currentBridge by rememberUpdatedState(bridge)
    val currentLook by rememberUpdatedState(look)
    val isUrl = content is HtmlContent.Url
    var size by remember { mutableStateOf(IntSize.Zero) }
    var browser by remember { mutableStateOf<OffscreenBrowser?>(null) }
    val focus = remember { FocusRequester() }
    val address = remember(content, policy, look, bridge != null) { addressOf(content, policy, look, bridge != null) }
    val ready = size.width > 0 && size.height > 0

    // The first picture needs a size, so the browser starts after the first layout.
    DisposableEffect(ready) {
        val events = object : BrowserEvents {
            override fun onFrame(image: Image, popup: Boolean) = frames.show(image, popup)
            override fun onPopup(bounds: Rectangle?) = SwingUtilities.invokeLater { frames.popupBounds = bounds }
            override fun onCursor(type: Int) = SwingUtilities.invokeLater { frames.cursor = type }
            override fun onNavigate(url: String) = currentNavigation.onNavigate(url)
            override fun onMessage(message: String) = SwingUtilities.invokeLater { currentBridge?.receive(message) }
            override fun onLoaded() = SwingUtilities.invokeLater {
                val page = browser?.browser ?: return@invokeLater
                // A page may have drawn all it shows before it finished loading; this asks for a picture of it as loaded.
                if (frames.loaded()) page.invalidate()
                if (isUrl) page.executeJavaScript(themeScript(currentLook), "", 0)
                val messages = currentBridge ?: return@invokeLater
                if (!policy.scripts && !isUrl) return@invokeLater
                page.executeJavaScript(desktopBridgeScript, "", 0)
                messages.attach { message -> onEventThread { page.executeJavaScript("window.stretch&&window.stretch._receive(${jsString(message)})", "", 0) } }
            }
        }
        if (ready) {
            onEventThread {
                frames.loading()
                browser = OffscreenBrowser(address, pointsOf(size.width, density), pointsOf(size.height, density), density.density, events)
            }
        }
        onDispose {
            currentBridge?.detach()
            val closing = browser
            browser = null
            onEventThread {
                closing?.close()
                frames.clear()
            }
        }
    }
    LaunchedEffect(browser, address) {
        val current = browser ?: return@LaunchedEffect
        onEventThread {
            if (current.browser.url != null && current.browser.url != address) {
                currentBridge?.detach()
                frames.loading()
                current.load(address)
            }
        }
    }
    LaunchedEffect(browser, look) {
        val page = browser?.browser ?: return@LaunchedEffect
        if (isUrl) onEventThread { page.executeJavaScript(themeScript(look), "", 0) }
    }
    LaunchedEffect(browser, size, density) {
        val current = browser ?: return@LaunchedEffect
        onEventThread { current.resize(pointsOf(size.width, density), pointsOf(size.height, density), density.density) }
    }

    // A page with no background of its own shows what is behind it: the theme for a document, white for a site, as on the web.
    val ground = if (content is HtmlContent.Inline) Stretch.colors.surfaceFloat else Color.White
    Box(
        modifier
            .background(ground)
            .clipToBounds()
            .onSizeChanged { size = it }
            .pointerHoverIcon(PointerIcon(Cursor(frames.cursor)))
            .browserPointerInput(density, { browser?.browser }, { focus.requestFocus() })
            .onFocusChanged { state -> browser?.browser?.let { page -> onEventThread { page.setFocus(state.isFocused) } } }
            .focusRequester(focus)
            .onPreviewKeyEvent { event -> forwardKey(event, browser?.browser) }
            .focusable()
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val target = canvas.nativeCanvas
                    frames.page?.let { image -> target.drawImageRect(image, Rect.makeWH(image.width.toFloat(), image.height.toFloat())) }
                    val popup = frames.popup
                    val bounds = frames.popupBounds
                    if (popup != null && bounds != null) {
                        val left = bounds.x * density.density
                        val top = bounds.y * density.density
                        target.drawImageRect(popup, Rect.makeXYWH(left, top, popup.width.toFloat(), popup.height.toFloat()))
                    }
                }
            },
    )
}
