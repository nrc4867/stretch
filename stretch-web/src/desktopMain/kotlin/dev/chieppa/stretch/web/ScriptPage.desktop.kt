package dev.chieppa.stretch.web

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val LAYOUT_WIDTH = 1600
private const val LAYOUT_HEIGHT = 1200
private const val FRAMES_PER_SECOND = 1
private const val BLANK_PAGE = "data:text/html;charset=utf-8,<!doctype html><html><body></body></html>"

/** Runs in an offscreen browser no one draws, with results coming back through JCEF's message router. */
actual class ScriptPage actual constructor() {
    private val lock = Mutex()
    private val loaded = CompletableDeferred<Unit>()
    private var pending: CompletableDeferred<String>? = null
    private var browser: OffscreenBrowser? = null

    private val events = object : BrowserEvents {
        override fun onLoaded() {
            loaded.complete(Unit)
        }

        override fun onMessage(message: String) {
            pending?.complete(message)
        }
    }

    private suspend fun page(): OffscreenBrowser {
        val page = withContext(Dispatchers.Swing) {
            browser ?: OffscreenBrowser(BLANK_PAGE, LAYOUT_WIDTH, LAYOUT_HEIGHT, 1f, events).also {
                it.browser.setWindowlessFrameRate(FRAMES_PER_SECOND)
                browser = it
            }
        }
        loaded.await()
        return page
    }

    private suspend fun run(script: String): String = lock.withLock {
        val page = page()
        val answer = CompletableDeferred<String>()
        pending = answer
        val call = reportingScript(script, "((m) => new Promise((ok, no) => window.${OffscreenBrowser.QUERY_FUNCTION}({ request: m, onSuccess: ok, onFailure: no })))")
        withContext(Dispatchers.Swing) { page.browser.executeJavaScript(call, "", 0) }
        answer.await()
    }

    actual suspend fun load(script: String) {
        outcomeOf(run(scriptElementLoader(script)))
    }

    actual suspend fun call(expression: String): String = outcomeOf(run(expression))

    actual fun close() {
        val page = browser ?: return
        browser = null
        javax.swing.SwingUtilities.invokeLater { page.close() }
    }
}
