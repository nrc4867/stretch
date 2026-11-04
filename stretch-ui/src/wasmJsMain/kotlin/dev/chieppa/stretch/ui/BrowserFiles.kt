@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.ui

import kotlinx.coroutines.await
import org.w3c.files.File
import org.w3c.files.FileList
import org.w3c.files.get
import kotlin.js.Promise

private const val PATH_PREFIX = "browser-file:"

/**
 * Files the person handed to this page, under made-up paths, since a browser never shows a file's real path. A path
 * ends in the file's name, so [fileExtension] reads it as usual, and lasts until the page closes.
 */
object BrowserFiles {
    private val files = mutableMapOf<String, File>()
    private var next = 0

    /** Keeps [file] and returns its path. */
    fun add(file: File): String = "$PATH_PREFIX${next++}/${file.name}".also { files[it] = file }

    /** The file at [path], or null if this page holds none there. */
    fun file(path: String): File? = files[path]
}

/** Asks for files with a file input: [accept] is its filter. Answers null when the person cancels. */
private fun showFileInput(accept: String, multiple: Boolean): Promise<FileList?> = js("""new Promise((resolve) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = accept;
    input.multiple = multiple;
    input.addEventListener('change', () => resolve(input.files));
    input.addEventListener('cancel', () => resolve(null));
    input.click();
})""")

/** The chosen files' paths in [BrowserFiles], with [extensions] listed in the picker; empty when cancelled. */
actual suspend fun pickFiles(title: String, extensions: Set<String>, multiple: Boolean): List<String> {
    val accept = extensions.joinToString(",") { ".$it" }
    val files = showFileInput(accept, multiple).await<FileList?>() ?: return emptyList()
    return (0 until files.length).mapNotNull { files[it]?.let(BrowserFiles::add) }
}
