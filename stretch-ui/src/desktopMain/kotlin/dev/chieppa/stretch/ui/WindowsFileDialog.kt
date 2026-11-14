package dev.chieppa.stretch.ui

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.WString
import com.sun.jna.platform.win32.COM.Unknown
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.Ole32
import com.sun.jna.platform.win32.WTypes
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import java.awt.Window

/** The shell's file dialogs and the interfaces they answer through, as Windows declares them. */
private object Shell {
    val FILE_OPEN_DIALOG: Guid.CLSID = Guid.CLSID("{DC1C5A9C-E88A-4dde-A5A1-60F82A20AEF7}")
    val FILE_SAVE_DIALOG: Guid.CLSID = Guid.CLSID("{C0B4E2F3-BA21-4773-8DBA-335EC946EB8B}")
    val I_FILE_OPEN_DIALOG: Guid.IID = Guid.IID("{d57c7288-d4ad-4768-be02-9d969532d960}")
    val I_FILE_SAVE_DIALOG: Guid.IID = Guid.IID("{84bccd23-5fde-4cdb-aea4-af64b83d78ab}")
    val I_SHELL_ITEM: Guid.IID = Guid.IID("{43826d1e-e718-42ee-bc55-a1e261c37bfe}")

    /** Places in IFileDialog's table of methods, after IUnknown's three and IModalWindow's Show. */
    const val SHOW = 3
    const val SET_FILE_TYPES = 4
    const val SET_OPTIONS = 9
    const val GET_OPTIONS = 10
    const val SET_FOLDER = 12
    const val GET_FOLDER = 13
    const val SET_FILE_NAME = 15
    const val GET_FILE_NAME = 16
    const val SET_TITLE = 17
    const val GET_RESULT = 20
    const val SET_DEFAULT_EXTENSION = 22

    /** IFileOpenDialog's own, after IFileDialog's. */
    const val GET_RESULTS = 27

    /** IShellItem's GetDisplayName and IShellItemArray's GetCount and GetItemAt. */
    const val ITEM_DISPLAY_NAME = 5
    const val ARRAY_COUNT = 7
    const val ARRAY_ITEM_AT = 8

    const val FOS_OVERWRITEPROMPT = 0x2
    const val FOS_PICKFOLDERS = 0x20
    const val FOS_FORCEFILESYSTEM = 0x40
    const val FOS_ALLOWMULTISELECT = 0x200
    const val FOS_PATHMUSTEXIST = 0x800
    const val FOS_FILEMUSTEXIST = 0x1000

    /** SIGDN_FILESYSPATH: an item's name as a path on disk. */
    const val SIGDN_FILESYSPATH = 0x80058000.toInt()

    /** HRESULT_FROM_WIN32(ERROR_CANCELLED), what Show answers when the person cancels. */
    const val CANCELLED = 0x800704C7.toInt()

    const val COINIT_APARTMENTTHREADED = 0x2
}

/** shell32's SHCreateItemFromParsingName, which makes the shell item for a path. */
private interface ShellItems : Library {
    @Suppress("FunctionName")
    fun SHCreateItemFromParsingName(path: WString, bindContext: Pointer?, iid: Guid.GUID, item: PointerByReference): Int

    companion object {
        val INSTANCE: ShellItems by lazy { Native.load("shell32", ShellItems::class.java) }
    }
}

/** COMDLG_FILTERSPEC: one entry of a dialog's file types, its name and its patterns. */
@Structure.FieldOrder("name", "spec")
internal class FilterSpec : Structure() {
    @JvmField var name: WString? = null

    @JvmField var spec: WString? = null
}

/** A COM interface pointer whose methods are called by their place in its table. */
private class ComObject(pointer: Pointer) : Unknown(pointer) {
    fun call(method: Int, vararg args: Any?): Int = _invokeNativeInt(method, arrayOf<Any?>(pointer, *args))
}

/** The dialog's file types as Windows takes them: each kind's name and its extensions as patterns, such as "*.png;*.jpg". */
internal fun filterSpecs(kinds: List<FileKind>): List<Pair<String, String>> =
    kinds.map { kind -> kind.name to (kind.extensions.joinToString(";") { "*.$it" }.ifEmpty { "*.*" }) }

/**
 * The Windows shell's file dialogs, the ones Explorer uses, through COM. Called on one thread only, which this sets up for the
 * shell's objects the first time. [DialogOutcome.Unavailable] when the shell cannot make one.
 */
internal object WindowsFileDialog {
    private var initialized = false

    fun open(title: String, kinds: List<FileKind>, multiple: Boolean, owner: Window?): DialogOutcome<List<String>> {
        val dialog = create(Shell.FILE_OPEN_DIALOG, Shell.I_FILE_OPEN_DIALOG) ?: return DialogOutcome.Unavailable
        try {
            prepare(dialog, title, kinds, Shell.FOS_FORCEFILESYSTEM or Shell.FOS_PATHMUSTEXIST or Shell.FOS_FILEMUSTEXIST or (if (multiple) Shell.FOS_ALLOWMULTISELECT else 0))
            when (val shown = dialog.call(Shell.SHOW, ownerHandle(owner))) {
                0 -> Unit
                Shell.CANCELLED -> return DialogOutcome.Cancelled
                else -> return DialogOutcome.Unavailable.also { System.err.println("File dialog failed: 0x${shown.toUInt().toString(16)}") }
            }
            val results = PointerByReference()
            if (dialog.call(Shell.GET_RESULTS, results) != 0) return DialogOutcome.Cancelled
            val array = ComObject(results.value)
            try {
                val count = IntByReference()
                array.call(Shell.ARRAY_COUNT, count)
                val paths = (0 until count.value).mapNotNull { index ->
                    val item = PointerByReference()
                    if (array.call(Shell.ARRAY_ITEM_AT, index, item) != 0) null else pathOf(ComObject(item.value))
                }
                return DialogOutcome.Chosen(paths)
            } finally {
                array.Release()
            }
        } finally {
            dialog.Release()
        }
    }

    fun save(title: String, kinds: List<FileKind>, suggestedName: String, defaultExtension: String, owner: Window?): DialogOutcome<String> {
        val dialog = create(Shell.FILE_SAVE_DIALOG, Shell.I_FILE_SAVE_DIALOG) ?: return DialogOutcome.Unavailable
        try {
            prepare(dialog, title, kinds, Shell.FOS_FORCEFILESYSTEM or Shell.FOS_PATHMUSTEXIST or Shell.FOS_OVERWRITEPROMPT)
            if (suggestedName.isNotBlank()) dialog.call(Shell.SET_FILE_NAME, WString(suggestedName))
            if (defaultExtension.isNotEmpty()) dialog.call(Shell.SET_DEFAULT_EXTENSION, WString(defaultExtension))
            when (val shown = dialog.call(Shell.SHOW, ownerHandle(owner))) {
                0 -> Unit
                Shell.CANCELLED -> return DialogOutcome.Cancelled
                else -> return DialogOutcome.Unavailable.also { System.err.println("File dialog failed: 0x${shown.toUInt().toString(16)}") }
            }
            val result = PointerByReference()
            if (dialog.call(Shell.GET_RESULT, result) != 0) return DialogOutcome.Cancelled
            return pathOf(ComObject(result.value))?.let { DialogOutcome.Chosen(it) } ?: DialogOutcome.Cancelled
        } finally {
            dialog.Release()
        }
    }

    /** A folder chosen with the open dialog set to pick folders, starting in [start] when it names one. */
    fun chooseFolder(title: String, start: String?, owner: Window?): DialogOutcome<String> {
        val dialog = create(Shell.FILE_OPEN_DIALOG, Shell.I_FILE_OPEN_DIALOG) ?: return DialogOutcome.Unavailable
        try {
            prepare(dialog, title, emptyList(), FOLDER_OPTIONS)
            if (start != null) startIn(dialog, start)
            when (val shown = dialog.call(Shell.SHOW, ownerHandle(owner))) {
                0 -> Unit
                Shell.CANCELLED -> return DialogOutcome.Cancelled
                else -> return DialogOutcome.Unavailable.also { System.err.println("Folder dialog failed: 0x${shown.toUInt().toString(16)}") }
            }
            val result = PointerByReference()
            if (dialog.call(Shell.GET_RESULT, result) != 0) return DialogOutcome.Cancelled
            return pathOf(ComObject(result.value))?.let { DialogOutcome.Chosen(it) } ?: DialogOutcome.Cancelled
        } finally {
            dialog.Release()
        }
    }

    /** Makes a folder dialog starting in [start] and reads back, without showing it, its options and the folder it would open in. */
    fun probeFolder(start: String): Pair<Int, String?>? {
        val dialog = create(Shell.FILE_OPEN_DIALOG, Shell.I_FILE_OPEN_DIALOG) ?: return null
        try {
            prepare(dialog, "", emptyList(), FOLDER_OPTIONS)
            startIn(dialog, start)
            val options = IntByReference()
            dialog.call(Shell.GET_OPTIONS, options)
            val folder = PointerByReference()
            if (dialog.call(Shell.GET_FOLDER, folder) != 0 || folder.value == null) return options.value to null
            return options.value to pathOf(ComObject(folder.value))
        } finally {
            dialog.Release()
        }
    }

    /** Makes a dialog and reads back what it was given, without showing it: its options and file name, for checking the binding. */
    fun probe(fileName: String): Pair<Int, String?>? {
        val dialog = create(Shell.FILE_SAVE_DIALOG, Shell.I_FILE_SAVE_DIALOG) ?: return null
        try {
            prepare(dialog, "", listOf(FileKind("Images", setOf("png", "jpg"))), Shell.FOS_OVERWRITEPROMPT)
            dialog.call(Shell.SET_FILE_NAME, WString(fileName))
            val options = IntByReference()
            dialog.call(Shell.GET_OPTIONS, options)
            val name = PointerByReference()
            if (dialog.call(Shell.GET_FILE_NAME, name) != 0) return options.value to null
            return options.value to takeString(name.value)
        } finally {
            dialog.Release()
        }
    }

    private val FOLDER_OPTIONS = Shell.FOS_PICKFOLDERS or Shell.FOS_FORCEFILESYSTEM or Shell.FOS_PATHMUSTEXIST

    /** Opens [dialog] in the folder at [path]; a path the shell cannot read leaves it where it would open anyway. */
    private fun startIn(dialog: ComObject, path: String) {
        val item = PointerByReference()
        val made = runCatching { ShellItems.INSTANCE.SHCreateItemFromParsingName(WString(path), null, Shell.I_SHELL_ITEM, item) }.getOrNull()
        if (made != 0 || item.value == null) return
        val folder = ComObject(item.value)
        try {
            dialog.call(Shell.SET_FOLDER, folder.pointer)
        } finally {
            folder.Release()
        }
    }

    private fun create(clsid: Guid.CLSID, iid: Guid.IID): ComObject? {
        if (!initialized) {
            val started = Ole32.INSTANCE.CoInitializeEx(null, Shell.COINIT_APARTMENTTHREADED)
            initialized = started.toInt() >= 0
            if (!initialized) return null
        }
        val made = PointerByReference()
        val result = Ole32.INSTANCE.CoCreateInstance(clsid, null, WTypes.CLSCTX_INPROC_SERVER, iid, made)
        return if (result.toInt() == 0 && made.value != null) ComObject(made.value) else null
    }

    private fun prepare(dialog: ComObject, title: String, kinds: List<FileKind>, flags: Int) {
        if (title.isNotEmpty()) dialog.call(Shell.SET_TITLE, WString(title))
        val specs = filterSpecs(kinds)
        if (specs.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            val entries = FilterSpec().toArray(specs.size) as Array<FilterSpec>
            specs.forEachIndexed { i, (name, spec) ->
                entries[i].name = WString(name)
                entries[i].spec = WString(spec)
                entries[i].write()
            }
            dialog.call(Shell.SET_FILE_TYPES, specs.size, entries[0].pointer)
        }
        val options = IntByReference()
        dialog.call(Shell.GET_OPTIONS, options)
        dialog.call(Shell.SET_OPTIONS, options.value or flags)
    }

    private fun pathOf(item: ComObject): String? = try {
        val name = PointerByReference()
        if (item.call(Shell.ITEM_DISPLAY_NAME, Shell.SIGDN_FILESYSPATH, name) != 0) null else takeString(name.value)
    } finally {
        item.Release()
    }

    /** The string at [pointer], which the shell allocated, freed once read. */
    private fun takeString(pointer: Pointer?): String? {
        if (pointer == null) return null
        return try {
            pointer.getWideString(0)
        } finally {
            Ole32.INSTANCE.CoTaskMemFree(pointer)
        }
    }

    private fun ownerHandle(owner: Window?): Pointer? =
        owner?.takeIf { it.isDisplayable }?.let { runCatching { Native.getWindowPointer(it) }.getOrNull() }
}
