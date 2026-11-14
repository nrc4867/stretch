package dev.chieppa.stretch.ui

import java.awt.FileDialog
import java.awt.Frame
import java.awt.KeyboardFocusManager
import java.awt.Window
import java.io.File
import javax.swing.JFileChooser
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext

/** A kind of file a dialog lists: its [name] as the dialog shows it, and the [extensions], without dots, it covers. */
data class FileKind(val name: String, val extensions: Set<String>) {
    /** Whether [fileName] has one of [extensions], or anything when there are none. */
    fun covers(fileName: String): Boolean = extensions.isEmpty() || fileExtension(fileName) in extensions
}

/**
 * The platform's own dialogs for choosing files and folders. On Windows they are the system's file dialogs, with the address bar,
 * search, previews and a list of [FileKind]s; elsewhere they are AWT's, and Swing's for a folder outside macOS, where AWT's dialog
 * picks files only. Each is modal over [owner], the app's window, or over the active window when null.
 */
object SystemFileDialogs {
    /** Shows a Windows dialog on a thread of its own, set up for the shell's objects, so the app's window keeps drawing under it. */
    private val windowsThread by lazy {
        Executors.newSingleThreadExecutor { task -> Thread(task, "file dialog").apply { isDaemon = true } }.asCoroutineDispatcher()
    }

    private val onWindows: Boolean = System.getProperty("os.name").orEmpty().startsWith("Windows")
    private val onMac: Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac")

    /** The property that turns macOS's AWT file dialog into a folder dialog while it is set. */
    private const val MAC_FOLDER_DIALOG = "apple.awt.fileDialogForDirectories"

    /** The files chosen to open, listing [kinds], several at once when [multiple]; empty when cancelled. */
    suspend fun open(title: String, kinds: List<FileKind>, multiple: Boolean, owner: Window? = null): List<String> {
        val parent = owner ?: activeWindow()
        if (onWindows) {
            when (val shown = withContext(windowsThread) { WindowsFileDialog.open(title, kinds, multiple, parent) }) {
                is DialogOutcome.Chosen -> return shown.value
                DialogOutcome.Cancelled -> return emptyList()
                DialogOutcome.Unavailable -> Unit
            }
        }
        return awtOpen(title, kinds, multiple, parent)
    }

    /**
     * Where to save, listing [kinds], starting from [suggestedName]; null when cancelled. A name typed without an extension gets
     * [defaultExtension], and an existing file is asked about before it is chosen.
     */
    suspend fun save(title: String, kinds: List<FileKind>, suggestedName: String, defaultExtension: String, owner: Window? = null): String? {
        val parent = owner ?: activeWindow()
        if (onWindows) {
            when (val shown = withContext(windowsThread) { WindowsFileDialog.save(title, kinds, suggestedName, defaultExtension, parent) }) {
                is DialogOutcome.Chosen -> return withExtension(shown.value, defaultExtension)
                DialogOutcome.Cancelled -> return null
                DialogOutcome.Unavailable -> Unit
            }
        }
        return awtSave(title, kinds, suggestedName, defaultExtension, parent)?.let { withExtension(it, defaultExtension) }
    }

    /** A folder chosen in a dialog that opens in [start] when it names one; null when cancelled. */
    suspend fun chooseFolder(title: String, start: String? = null, owner: Window? = null): String? {
        val parent = owner ?: activeWindow()
        val from = start?.takeIf { it.isNotBlank() && File(it).isDirectory }
        if (onWindows) {
            when (val shown = withContext(windowsThread) { WindowsFileDialog.chooseFolder(title, from, parent) }) {
                is DialogOutcome.Chosen -> return shown.value
                DialogOutcome.Cancelled -> return null
                DialogOutcome.Unavailable -> Unit
            }
        }
        return if (onMac) macFolder(title, from, parent) else swingFolder(title, from, parent)
    }

    private fun activeWindow(): Window? = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow

    private fun awtOpen(title: String, kinds: List<FileKind>, multiple: Boolean, owner: Window?): List<String> {
        val dialog = FileDialog(owner as? Frame, title, FileDialog.LOAD)
        dialog.isMultipleMode = multiple
        if (kinds.isNotEmpty()) dialog.setFilenameFilter { _, name -> kinds.any { it.covers(name) } }
        dialog.isVisible = true
        return dialog.files.map { it.path }
    }

    private fun macFolder(title: String, start: String?, owner: Window?): String? {
        val before = System.getProperty(MAC_FOLDER_DIALOG)
        System.setProperty(MAC_FOLDER_DIALOG, "true")
        try {
            val dialog = FileDialog(owner as? Frame, title, FileDialog.LOAD)
            if (start != null) dialog.directory = start
            dialog.isVisible = true
            val chosen = dialog.file ?: return null
            return File(dialog.directory, chosen).path
        } finally {
            if (before == null) System.clearProperty(MAC_FOLDER_DIALOG) else System.setProperty(MAC_FOLDER_DIALOG, before)
        }
    }

    private fun swingFolder(title: String, start: String?, owner: Window?): String? {
        val chooser = JFileChooser(start)
        chooser.dialogTitle = title
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        return if (chooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION) chooser.selectedFile?.path else null
    }

    private fun awtSave(title: String, kinds: List<FileKind>, suggestedName: String, defaultExtension: String, owner: Window?): String? {
        val dialog = FileDialog(owner as? Frame, title, FileDialog.SAVE)
        dialog.file = withExtension(suggestedName.ifBlank { "*" }, defaultExtension)
        if (kinds.isNotEmpty()) dialog.setFilenameFilter { _, name -> kinds.any { it.covers(name) } }
        dialog.isVisible = true
        val chosen = dialog.file ?: return null
        return File(dialog.directory, chosen).path
    }
}

/** What a system dialog came back with: the [Chosen] files, nothing when [Cancelled], or [Unavailable] when it could not be shown. */
internal sealed interface DialogOutcome<out T> {
    data class Chosen<T>(val value: T) : DialogOutcome<T>

    data object Cancelled : DialogOutcome<Nothing>

    data object Unavailable : DialogOutcome<Nothing>
}

/** [path] ending in [extension], added when it has none of its own. */
internal fun withExtension(path: String, extension: String): String =
    if (extension.isEmpty() || path.endsWith(".$extension", ignoreCase = true)) path else "$path.$extension"
