@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.document

import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import kotlin.js.Promise

private const val PATH_PREFIX = "browser-handle:"
private const val HANDLES_DATABASE = "stretch-file-handles"

private external interface FileHandle : JsAny

private fun newId(): String = js("crypto.randomUUID()")

private fun handleName(handle: FileHandle): String = js("handle.name")

/** Whether this browser can read and write files the person picks. */
internal fun fileSystemAccessSupported(): Boolean = js("typeof window.showSaveFilePicker === 'function'")

/** The picker's one file type, named [description], taking the comma-separated [extensions]. */
private fun pickerTypes(extensions: String, description: String): JsAny = js("""[{
    description: description,
    accept: { 'application/octet-stream': extensions.split(',').map((e) => '.' + e) },
}]""")

private fun pickerTypes(extensions: Set<String>, description: String): JsAny = pickerTypes(extensions.joinToString(","), description)

/** Null when the person cancels. */
private fun openPicker(types: JsAny): Promise<FileHandle?> = js("""window.showOpenFilePicker({ types: types })
    .then((handles) => handles[0])
    .catch((e) => { if (e.name === 'AbortError') return null; throw e; })""")

/** Null when the person cancels. */
private fun savePicker(types: JsAny, name: String): Promise<FileHandle?> = js("""window.showSaveFilePicker({ types: types, suggestedName: name })
    .catch((e) => { if (e.name === 'AbortError') return null; throw e; })""")

/** The handle's file as bytes, asking for permission if this visit has none yet. */
private fun readHandle(handle: FileHandle): Promise<ArrayBuffer> = js("""handle.requestPermission({ mode: 'read' })
    .then(() => handle.getFile())
    .then((file) => file.arrayBuffer())""")

/** The browser swaps the file in when the stream closes, so a failed write leaves it as it was. */
private fun writeHandle(handle: FileHandle, bytes: Uint8Array): Promise<JsAny?> = js("""handle.requestPermission({ mode: 'readwrite' })
    .then(() => handle.createWritable())
    .then((stream) => stream.write(bytes).then(() => stream.close()))
    .then(() => null)""")

/** False once the file is gone. A file this visit may not look at yet counts as there. */
private fun handleExists(handle: FileHandle): Promise<JsBoolean> = js("""handle.queryPermission({ mode: 'read' })
    .then((state) => state !== 'granted' ? true : handle.getFile().then(() => true))
    .catch(() => false)""")

/**
 * Files picked through the File System Access API, under made-up paths ending in the file's name. Handles stay in
 * IndexedDB, so a path, such as one in the recent files, works on a later visit once the person allows it again.
 */
internal object BrowserFileHandles {
    private val store = BrowserStore(HANDLES_DATABASE)

    fun owns(path: String): Boolean = path.startsWith(PATH_PREFIX)

    private suspend fun keep(handle: FileHandle): String {
        val path = "$PATH_PREFIX${newId()}/${handleName(handle)}"
        store.put(path, handle)
        return path
    }

    private suspend fun handle(path: String): FileHandle? = store.get(path)?.unsafeCast()

    suspend fun chooseOpen(format: DocumentFormat<*>): String? =
        openPicker(pickerTypes(format.openExtensions, format.description)).await<FileHandle?>()?.let { keep(it) }

    suspend fun chooseSave(format: DocumentFormat<*>, suggestedName: String): String? {
        val name = "${suggestedName.ifBlank { format.description }}.${format.extension}"
        return savePicker(pickerTypes(setOf(format.extension), format.description), name).await<FileHandle?>()?.let { keep(it) }
    }

    suspend fun read(path: String): ByteArray {
        val handle = checkNotNull(handle(path)) { "No such file: $path" }
        return readHandle(handle).await<ArrayBuffer>().toByteArray()
    }

    suspend fun write(path: String, bytes: ByteArray) {
        val handle = checkNotNull(handle(path)) { "No such file: $path" }
        writeHandle(handle, bytes.toUint8Array()).await<JsAny?>()
    }

    suspend fun exists(path: String): Boolean {
        val handle = handle(path) ?: return false
        return handleExists(handle).await<JsBoolean>().toBoolean()
    }
}
