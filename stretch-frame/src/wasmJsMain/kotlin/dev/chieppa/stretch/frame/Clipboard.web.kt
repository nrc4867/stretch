@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.frame

import kotlinx.coroutines.await
import kotlin.js.Promise

private fun writeClipboardText(text: String): Unit = js("void navigator.clipboard.writeText(text).catch(() => {})")

private fun readClipboardText(): Promise<JsString> = js("navigator.clipboard.readText().catch(() => '')")

internal actual fun systemClipboard(): ClipboardAccess = BrowserClipboard

/**
 * The browser's clipboard, which holds text only. Custom data stays in this page, and is kept with a paste as long as
 * the clipboard still holds the text written with it.
 */
private object BrowserClipboard : ClipboardAccess {
    private var written: ClipData? = null
    private var writtenText = ""

    override suspend fun read(types: Set<String>): ClipData? {
        val text = readClipboardText().await<JsString>().toString()
        val own = written
        return when {
            own != null && text == writtenText -> own.narrowedTo(types)
            text.isEmpty() -> null
            else -> ClipData(text)
        }
    }

    override fun write(data: ClipData) {
        written = data
        writtenText = data.text.orEmpty()
        writeClipboardText(writtenText)
    }
}
