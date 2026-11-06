package dev.chieppa.stretch.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/**
 * Messages between an [HtmlView]'s page and the app, as strings (encode anything richer as JSON). The page sends with
 * `stretch.post(text)` and hears the app through `stretch.listen(fn)`, which returns a function that stops listening;
 * `window.stretch` exists once the page fires `stretch-ready` on `window`. The app sends with [post] and hears the page
 * through [onMessage], on the UI thread. Messages posted before a page is ready wait for it.
 */
@Stable
class HtmlBridge(onMessage: (String) -> Unit) {
    internal var onMessage: (String) -> Unit = onMessage
    private val waiting = ArrayDeque<String>()
    private var send: ((String) -> Unit)? = null

    /** Sends [message] to the page's listeners, now or once the page is ready. */
    fun post(message: String) {
        val to = send
        if (to != null) to(message) else waiting.addLast(message)
    }

    /** A page is ready: messages go to it through [to], those waiting first. */
    internal fun attach(to: (String) -> Unit) {
        send = to
        while (waiting.isNotEmpty()) to(waiting.removeFirst())
    }

    /** The page is going away; messages wait for the next one. */
    internal fun detach() {
        send = null
    }

    internal fun receive(message: String) = onMessage(message)
}

/** An [HtmlBridge] kept across recompositions, always calling the latest [onMessage]. */
@Composable
fun rememberHtmlBridge(onMessage: (String) -> Unit): HtmlBridge {
    val bridge = remember { HtmlBridge(onMessage) }
    SideEffect { bridge.onMessage = onMessage }
    return bridge
}

/** The event the page's `window` fires once `window.stretch` is there. */
internal const val BRIDGE_READY_EVENT = "stretch-ready"

/**
 * The script that gives a page `window.stretch`. [send] is a JS expression taking the text `m` that carries it to the
 * app. It does nothing when the page has it already.
 */
internal fun bridgeScript(send: String, extra: String = ""): String =
    "(function(){if(window.stretch)return;var ls=[];window.stretch={" +
        "post:function(m){m=String(m);$send;}," +
        "listen:function(f){ls.push(f);return function(){var i=ls.indexOf(f);if(i>=0)ls.splice(i,1);};}," +
        "_receive:function(m){ls.slice().forEach(function(f){try{f(m);}catch(e){console.error(e);}});}};" +
        extra +
        "window.dispatchEvent(new Event('$BRIDGE_READY_EVENT'));})();"

/** [text] as a JavaScript string literal. */
internal fun jsString(text: String): String = buildString {
    append('"')
    for (char in text) {
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '<' -> append("\\u003c")
            '\u2028' -> append("\\u2028")
            '\u2029' -> append("\\u2029")
            else -> if (char < ' ') append("\\u" + char.code.toString(16).padStart(4, '0')) else append(char)
        }
    }
    append('"')
}

/** A script that sets the theme's custom properties on the page's root, for a page Stretch did not write the head of. */
internal fun themeScript(look: HtmlLook): String =
    "(function(){var s=document.documentElement.style;" +
        look.variables.entries.joinToString("") { (name, value) -> "s.setProperty(${jsString(name)},${jsString(value)});" } +
        "})();"
