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
import dev.chieppa.stretch.core.Requirement
import dev.chieppa.stretch.core.RequirementState
import dev.chieppa.stretch.theme.Stretch
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event

actual object WebEngine {
    actual val available: Boolean = true

    actual val requirement: Requirement = object : Requirement {
        override val id = "stretch-web.engine"
        override val downloadBytes: Long? = null
        override val state: StateFlow<RequirementState> = MutableStateFlow(RequirementState.Ready)
        override fun install() = Unit
        override fun cancel() = Unit
        override fun restart() = Unit
    }
}

private const val NAVIGATE_PREFIX = "stretch-navigate:"

/** Sends link clicks up to the view, since the view cannot see into a sandboxed frame. Needs scripts, so only documents that allow them get it. */
private const val LINK_SCRIPT = "<script>addEventListener('click',function(e){" +
    "var a=e.target&&e.target.closest&&e.target.closest('a[href]');if(!a)return;" +
    "var h=a.getAttribute('href');if(h&&h.charAt(0)==='#')return;" +
    "e.preventDefault();parent.postMessage('$NAVIGATE_PREFIX'+a.href,'*');},true);</script>"

/** What a page posts to carry a bridge message, before the text. */
private const val MESSAGE_PREFIX = "stretch-message:"

/** What a bridged page posts once `window.stretch` is there. */
private const val READY_MESSAGE = "stretch-bridge-ready"

/** The page's side of an [HtmlBridge] in a frame: messages go both ways through `postMessage`. */
private val frameBridgeScript = bridgeScript(
    "parent.postMessage('$MESSAGE_PREFIX'+m,'*')",
    "addEventListener('message',function(e){if(e.source===parent&&typeof e.data==='string'&&e.data.indexOf('$MESSAGE_PREFIX')===0)" +
        "window.stretch._receive(e.data.slice(${MESSAGE_PREFIX.length}));});parent.postMessage('$READY_MESSAGE','*');",
)

private fun messageText(event: JsAny): String? = js("typeof event.data === 'string' ? event.data : null")

private fun postToFrame(frame: HTMLIFrameElement, message: String): Unit = js("frame.contentWindow && frame.contentWindow.postMessage(message, '*')")

private fun isFrom(event: JsAny, frame: HTMLIFrameElement): Boolean = js("event.source === frame.contentWindow")

private fun iframeSource(content: HtmlContent, policy: HtmlPolicy, look: HtmlLook, bridged: Boolean): String = when (content) {
    is HtmlContent.Url -> content.url
    is HtmlContent.Inline -> {
        val document = prepareDocument(content.html, policy, content.baseUrl, look, if (bridged) frameBridgeScript else null)
        if (policy.scripts) "$LINK_SCRIPT$document" else document
    }
}

@Composable
actual fun HtmlView(
    content: HtmlContent,
    modifier: Modifier,
    policy: HtmlPolicy,
    navigation: HtmlNavigation,
    bridge: HtmlBridge?,
    unavailable: @Composable () -> Unit,
) {
    val look = currentHtmlLook
    val currentNavigation by rememberUpdatedState(navigation)
    val currentBridge by rememberUpdatedState(bridge)
    val frame = remember { document.createElement("iframe") as HTMLIFrameElement }
    val ground = if (content is HtmlContent.Inline) Stretch.colors.surfaceFloat else Color.White
    val source = remember(content, policy, look, bridge != null) { iframeSource(content, policy, look, bridge != null) }
    DisposableEffect(frame) {
        val listener: (Event) -> Unit = listener@{ event ->
            val message = event as MessageEvent
            if (!isFrom(message, frame)) return@listener
            val text = messageText(message) ?: return@listener
            when {
                text == READY_MESSAGE -> currentBridge?.attach { postToFrame(frame, MESSAGE_PREFIX + it) }
                text.startsWith(MESSAGE_PREFIX) -> currentBridge?.receive(text.removePrefix(MESSAGE_PREFIX))
            }
            if (!text.startsWith(NAVIGATE_PREFIX)) return@listener
            val target = text.removePrefix(NAVIGATE_PREFIX)
            if (currentNavigation.onNavigate(target)) frame.src = target
        }
        window.addEventListener("message", listener)
        onDispose {
            window.removeEventListener("message", listener)
            currentBridge?.detach()
        }
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
                    currentBridge?.detach()
                    it.removeAttribute("srcdoc")
                    it.setAttribute("src", source)
                }
                is HtmlContent.Inline -> if (it.getAttribute("srcdoc") != source) {
                    currentBridge?.detach()
                    it.removeAttribute("src")
                    it.setAttribute("srcdoc", source)
                }
            }
        },
    )
}
