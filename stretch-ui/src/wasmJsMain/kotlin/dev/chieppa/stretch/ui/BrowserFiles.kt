package dev.chieppa.stretch.ui

import org.w3c.files.File

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
