package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import dev.chieppa.stretch.core.Action
import dev.chieppa.stretch.core.ActionId
import dev.chieppa.stretch.core.KeyChord

/**
 * What goes on the clipboard: [text] for other apps, and the app's own data under types it names (such as
 * "stretch/layers"), so a copy keeps everything when pasted back into the app or another copy of it.
 */
class ClipData(val text: String? = null, val custom: Map<String, String> = emptyMap()) {
    init {
        require(text != null || custom.isNotEmpty()) { "ClipData needs text or custom data" }
    }
}

/** The system clipboard. Each platform supplies one; tests can provide their own through [LocalClipboardAccess]. */
interface ClipboardAccess {
    /** What is on the clipboard, with the [types] of custom data the app understands, or null when it is empty. */
    fun read(types: Set<String>): ClipData?

    fun write(data: ClipData)
}

/** Keeps the clipboard inside the app only: for tests and previews. */
class MemoryClipboard : ClipboardAccess {
    var content: ClipData? = null

    override fun read(types: Set<String>): ClipData? = content?.let { data ->
        val custom = data.custom.filterKeys { it in types }
        if (data.text == null && custom.isEmpty()) null else ClipData(data.text, custom)
    }

    override fun write(data: ClipData) {
        content = data
    }
}

internal expect fun systemClipboard(): ClipboardAccess

val LocalClipboardAccess = staticCompositionLocalOf { systemClipboard() }

/**
 * Copy, cut and paste for a part of the app that has a selection of its own, such as a canvas or a layer list.
 * [copy] returns what is selected, or null when nothing is. [cut] copies and removes it. [paste] takes clipboard data
 * holding text or one of [types] and returns whether it used it.
 */
interface ClipboardHandler {
    val types: Set<String> get() = emptySet()

    fun copy(): ClipData?

    fun cut(): ClipData? = null

    fun paste(data: ClipData): Boolean = false
}

/**
 * Sends Copy, Cut and Paste to whichever [clipboardTarget] holds the focus. Keys reach the focused target by
 * themselves; menus, the command palette and the macOS menu bar go through [copy], [cut] and [paste], or through
 * [actions]. Text fields keep their own clipboard keys.
 */
class ClipboardRouter(private val clipboard: () -> ClipboardAccess) {
    internal var focused by mutableStateOf<ClipboardHandler?>(null)

    /** Whether something has the focus that copy, cut and paste would act on. */
    val active: Boolean get() = focused != null

    fun copy(): Boolean = focused?.copy()?.also { clipboard().write(it) } != null

    fun cut(): Boolean = focused?.cut()?.also { clipboard().write(it) } != null

    fun paste(): Boolean {
        val handler = focused ?: return false
        val data = clipboard().read(handler.types) ?: return false
        return handler.paste(data)
    }

    /** Copy, Cut and Paste as actions with the usual keys, available while a target has the focus. */
    fun actions(copyText: String = "copy", cutText: String = "cut", pasteText: String = "paste"): List<Action> = listOf(
        Action(CopyAction, copyText, "copy", listOf(KeyChord.parse("Ctrl C")), isAvailable = { active }) { copy() },
        Action(CutAction, cutText, "cut", listOf(KeyChord.parse("Ctrl X")), isAvailable = { active }) { cut() },
        Action(PasteAction, pasteText, "paste", listOf(KeyChord.parse("Ctrl V")), isAvailable = { active }) { paste() },
    )

    companion object {
        val CopyAction = ActionId("copy")
        val CutAction = ActionId("cut")
        val PasteAction = ActionId("paste")
    }
}

@Composable
fun rememberClipboardRouter(): ClipboardRouter {
    val access by rememberUpdatedState(LocalClipboardAccess.current)
    return remember { ClipboardRouter { access } }
}

/**
 * Makes this part of the app a clipboard target for [router]: while the focus is inside it, Ctrl C, Ctrl X and
 * Ctrl V (Cmd on macOS) and the router's actions go to [handler]. Text fields inside keep their own clipboard.
 */
fun Modifier.clipboardTarget(router: ClipboardRouter, handler: ClipboardHandler): Modifier = composed {
    val current by rememberUpdatedState(handler)
    val proxy = remember {
        object : ClipboardHandler {
            override val types get() = current.types
            override fun copy() = current.copy()
            override fun cut() = current.cut()
            override fun paste(data: ClipData) = current.paste(data)
        }
    }
    DisposableEffect(router) { onDispose { if (router.focused === proxy) router.focused = null } }
    onFocusChanged { state ->
        if (state.hasFocus) router.focused = proxy else if (router.focused === proxy) router.focused = null
    }.onKeyEvent { event ->
        when (event.chord()) {
            CopyKey -> router.copy()
            CutKey -> router.cut()
            PasteKey -> router.paste()
            else -> false
        }
    }
}

private val CopyKey = KeyChord.parse("Ctrl C")
private val CutKey = KeyChord.parse("Ctrl X")
private val PasteKey = KeyChord.parse("Ctrl V")
