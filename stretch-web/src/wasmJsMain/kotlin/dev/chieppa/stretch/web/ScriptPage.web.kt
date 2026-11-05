@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.web

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

private const val READY_MESSAGE = "stretch-script-ready"
private const val FRAME_STYLE = "position:fixed;left:-20000px;top:0;width:1600px;height:1200px;border:0;visibility:hidden"

/** The hidden frame's own script: runs what it is sent and posts the outcome back. */
private const val FRAME_DOCUMENT = "<!doctype html><html><body><script>" +
    "addEventListener('message',function(e){(0,eval)(e.data)});" +
    "parent.postMessage('$READY_MESSAGE','*');" +
    "</script></body></html>"

private fun messageText(event: JsAny): String? = js("typeof event.data === 'string' ? event.data : null")

private fun isFrom(event: JsAny, frame: HTMLIFrameElement): Boolean = js("event.source === frame.contentWindow")

private fun post(frame: HTMLIFrameElement, message: String): Unit = js("frame.contentWindow.postMessage(message, '*')")

/** Runs in a hidden sandboxed frame, so a library never touches the app's page. Messages carry code in and results out. */
actual class ScriptPage actual constructor() {
    private val lock = Mutex()
    private val ready = CompletableDeferred<Unit>()
    private var pending: CompletableDeferred<String>? = null
    private var frame: HTMLIFrameElement? = null

    private val listener: (Event) -> Unit = listener@{ event ->
        val message = event as MessageEvent
        val current = frame ?: return@listener
        if (!isFrom(message, current)) return@listener
        val text = messageText(message) ?: return@listener
        if (text == READY_MESSAGE) ready.complete(Unit) else pending?.complete(text)
    }

    private suspend fun page(): HTMLIFrameElement {
        val existing = frame
        if (existing != null) {
            ready.await()
            return existing
        }
        val created = document.createElement("iframe") as HTMLIFrameElement
        created.setAttribute("sandbox", "allow-scripts")
        created.setAttribute("style", FRAME_STYLE)
        frame = created
        window.addEventListener("message", listener)
        created.setAttribute("srcdoc", FRAME_DOCUMENT)
        document.body?.appendChild(created)
        ready.await()
        return created
    }

    private suspend fun run(script: String): String = lock.withLock {
        val page = page()
        val answer = CompletableDeferred<String>()
        pending = answer
        post(page, reportingScript(script, "((m) => parent.postMessage(m, '*'))"))
        answer.await()
    }

    actual suspend fun load(script: String) {
        outcomeOf(run(scriptElementLoader(script)))
    }

    actual suspend fun call(expression: String): String = outcomeOf(run(expression))

    actual fun close() {
        window.removeEventListener("message", listener)
        frame?.remove()
        frame = null
    }
}
