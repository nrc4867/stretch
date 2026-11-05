@file:OptIn(ExperimentalWasmJsInterop::class, ExperimentalComposeUiApi::class)

package dev.chieppa.stretch.web

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.WebElementView
import dev.chieppa.stretch.theme.Stretch
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

actual object WebEngine {
    actual val available: Boolean get() = true
}

private const val NAVIGATE_PREFIX = "stretch-navigate:"

/** Sends link clicks up to the view, since the view cannot see into a sandboxed frame. Needs scripts, so only documents that allow them get it. */
private const val LINK_SCRIPT = "<script>addEventListener('click',function(e){" +
    "var a=e.target&&e.target.closest&&e.target.closest('a[href]');if(!a)return;" +
    "var h=a.getAttribute('href');if(h&&h.charAt(0)==='#')return;" +
    "e.preventDefault();parent.postMessage('$NAVIGATE_PREFIX'+a.href,'*');},true);</script>"

private fun messageText(event: JsAny): String? = js("typeof event.data === 'string' ? event.data : null")

private fun isFrom(event: JsAny, frame: HTMLIFrameElement): Boolean = js("event.source === frame.contentWindow")

private fun iframeSource(content: HtmlContent, policy: HtmlPolicy, look: HtmlLook): String = when (content) {
    is HtmlContent.Url -> content.url
    is HtmlContent.Inline -> {
        val document = prepareDocument(content.html, policy, content.baseUrl, look)
        if (policy.scripts) "$LINK_SCRIPT$document" else document
    }
}

@Composable
actual fun HtmlView(
    content: HtmlContent,
    modifier: Modifier,
    policy: HtmlPolicy,
    navigation: HtmlNavigation,
    unavailable: @Composable () -> Unit,
) {
    val look = currentHtmlLook
    val currentNavigation by rememberUpdatedState(navigation)
    val frame = remember { document.createElement("iframe") as HTMLIFrameElement }
    val ground = if (content is HtmlContent.Inline) Stretch.colors.surfaceFloat else Color.White
    val source = remember(content, policy, look) { iframeSource(content, policy, look) }
    DisposableEffect(frame) {
        val listener: (Event) -> Unit = listener@{ event ->
            val message = event as MessageEvent
            if (!isFrom(message, frame)) return@listener
            val text = messageText(message) ?: return@listener
            if (!text.startsWith(NAVIGATE_PREFIX)) return@listener
            val target = text.removePrefix(NAVIGATE_PREFIX)
            if (currentNavigation.onNavigate(target)) frame.src = target
        }
        window.addEventListener("message", listener)
        onDispose { window.removeEventListener("message", listener) }
    }
    WebElementView(
        factory = {
            frame.apply {
                style.border = "0"
                style.width = "100%"
                style.height = "100%"
            }
        },
        modifier = modifier.background(ground),
        update = {
            it.setAttribute("sandbox", if (policy.scripts) "allow-scripts" else "")
            when (content) {
                is HtmlContent.Url -> if (it.getAttribute("src") != source) {
                    it.removeAttribute("srcdoc")
                    it.setAttribute("src", source)
                }
                is HtmlContent.Inline -> if (it.getAttribute("srcdoc") != source) {
                    it.removeAttribute("src")
                    it.setAttribute("srcdoc", source)
                }
            }
        },
    )
}
