@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.document

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.chieppa.stretch.ui.BrowserFiles
import dev.chieppa.stretch.ui.fileName
import dev.chieppa.stretch.ui.pickFiles
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.files.File
import kotlin.js.Promise

private const val DOWNLOAD_PREFIX = "browser-download:"

private fun fileBytes(file: File): Promise<ArrayBuffer> = js("file.arrayBuffer()")

private fun download(name: String, bytes: Uint8Array): Unit = js("""{
    const url = URL.createObjectURL(new Blob([bytes]));
    const link = document.createElement('a');
    link.href = url;
    link.download = name;
    link.click();
    URL.revokeObjectURL(url);
}""")

/**
 * Documents in the files the person picks. Where the browser can't write to a picked file, saving downloads it, and a
 * file opened there holds only what the page was given.
 */
object BrowserStorage : DocumentStorage {
    override suspend fun read(path: String): ByteArray {
        if (BrowserFileHandles.owns(path)) return BrowserFileHandles.read(path)
        val file = checkNotNull(BrowserFiles.file(path)) { "No such file: $path" }
        return fileBytes(file).await<ArrayBuffer>().toByteArray()
    }

    override suspend fun write(path: String, bytes: ByteArray) {
        if (BrowserFileHandles.owns(path)) BrowserFileHandles.write(path, bytes) else download(fileName(path), bytes.toUint8Array())
    }

    override suspend fun exists(path: String): Boolean =
        if (BrowserFileHandles.owns(path)) BrowserFileHandles.exists(path) else BrowserFiles.file(path) != null

    override fun displayName(path: String): String = fileName(path).substringBeforeLast('.')
}

actual val documentStorage: DocumentStorage = BrowserStorage

/** The File System Access API's pickers, or a file input and downloads where the browser has none. */
private object BrowserFileChooser : FileChooser {
    override suspend fun chooseOpen(format: DocumentFormat<*>): String? =
        if (fileSystemAccessSupported()) {
            BrowserFileHandles.chooseOpen(format)
        } else {
            pickFiles(format.description, setOf(format.extension), multiple = false).firstOrNull()
        }

    override suspend fun chooseSave(format: DocumentFormat<*>, suggestedName: String): String? =
        if (fileSystemAccessSupported()) {
            BrowserFileHandles.chooseSave(format, suggestedName)
        } else {
            "$DOWNLOAD_PREFIX${suggestedName.ifBlank { format.description }}.${format.extension}"
        }
}

/** The browser's own file pickers, which title themselves. */
@Composable
actual fun rememberFileChooser(): FileChooser = remember { BrowserFileChooser }
