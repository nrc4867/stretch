package dev.chieppa.stretch.web

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** A script threw, or its promise rejected, in a [ScriptPage]. */
class ScriptException(message: String) : Exception(message)

/**
 * A page no one sees, for running a JavaScript library: a laid-out document with a DOM, so a library can measure text.
 * Calls run one at a time. Only available where [WebEngine.available] is true.
 */
expect class ScriptPage() {
    /** Runs [script] once at the top level of the page, as a `<script>` would. */
    suspend fun load(script: String)

    /** The string value of JavaScript [expression], waiting for it if it is a promise. Throws [ScriptException]. */
    suspend fun call(expression: String): String

    fun close()
}

/** Wraps [expression] so its outcome reaches [report], a JS function taking a string: "ok:" and the value, or "err:" and the message. */
internal fun reportingScript(expression: String, report: String): String =
    "(async () => { try { const r = await ($expression); $report('ok:' + r); } catch (e) { $report('err:' + (e && e.message || e)); } })();"

/**
 * An expression that runs [script] in a real `<script>` element, so its globals stay and its `var`s are globals, whatever
 * mode it asks for. Base64 keeps its text safe in transit, whatever a library holds.
 */
@OptIn(ExperimentalEncodingApi::class)
internal fun scriptElementLoader(script: String): String {
    val encoded = Base64.encode(script.encodeToByteArray())
    return "(() => { const s = document.createElement('script'); " +
        "s.text = new TextDecoder().decode(Uint8Array.from(atob('$encoded'), (c) => c.charCodeAt(0))); " +
        "document.head.appendChild(s); return ''; })()"
}

/** The value of an outcome from [reportingScript]. Throws [ScriptException] for a failure. */
internal fun outcomeOf(report: String): String = when {
    report.startsWith("ok:") -> report.removePrefix("ok:")
    else -> throw ScriptException(report.removePrefix("err:"))
}
