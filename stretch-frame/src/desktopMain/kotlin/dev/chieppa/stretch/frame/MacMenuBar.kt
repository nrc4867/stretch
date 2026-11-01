package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import dev.chieppa.stretch.core.ActionId
import dev.chieppa.stretch.core.ActionRegistry
import dev.chieppa.stretch.core.KeyChord
import dev.chieppa.stretch.core.Keymap
import java.awt.Desktop
import java.awt.event.KeyEvent as AwtKeyEvent

/** One menu in the macOS menu bar: its title and its actions in order, null for a divider. */
class AppMenu(val title: String, val items: List<ActionId?>)

/**
 * The macOS menu bar, built from the app's actions: each item shows its action's label and current shortcut, and
 * only runs when the action is available in [mode]. The app menu's About, Settings and Quit run [onAbout],
 * [onSettings] and [onQuit]; without them the system's defaults stay. On Windows and Linux it shows nothing: the top
 * bar and the command palette already hold the same actions.
 */
@Composable
fun FrameWindowScope.MacMenuBar(
    menus: List<AppMenu>,
    registry: ActionRegistry,
    keymap: Keymap,
    mode: String?,
    onAbout: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    onQuit: (() -> Unit)? = null,
) {
    if (!isMac) return
    AppMenuHandlers(onAbout, onSettings, onQuit)
    val text = LocalActionText.current
    MenuBar {
        for (menu in menus) {
            Menu(menu.title) {
                for (id in menu.items) {
                    if (id == null) {
                        Separator()
                        continue
                    }
                    val action = registry[id] ?: continue
                    Item(
                        text(action.textKey),
                        shortcut = keymap.keysFor(id).firstOrNull()?.let(::menuShortcut),
                        onClick = { if ((action.mode == null || action.mode == mode) && action.isAvailable()) action.run() },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppMenuHandlers(onAbout: (() -> Unit)?, onSettings: (() -> Unit)?, onQuit: (() -> Unit)?) {
    val about by rememberUpdatedState(onAbout)
    val settings by rememberUpdatedState(onSettings)
    val quit by rememberUpdatedState(onQuit)
    DisposableEffect(onAbout != null, onSettings != null, onQuit != null) {
        val desktop = Desktop.getDesktop()
        fun supports(action: Desktop.Action) = Desktop.isDesktopSupported() && desktop.isSupported(action)
        if (onAbout != null && supports(Desktop.Action.APP_ABOUT)) desktop.setAboutHandler { about?.invoke() }
        if (onSettings != null && supports(Desktop.Action.APP_PREFERENCES)) desktop.setPreferencesHandler { settings?.invoke() }
        if (onQuit != null && supports(Desktop.Action.APP_QUIT_HANDLER)) {
            desktop.setQuitHandler { _, response ->
                // The app decides: it may ask to save first and close its windows itself.
                response.cancelQuit()
                quit?.invoke()
            }
        }
        onDispose {
            if (onAbout != null && supports(Desktop.Action.APP_ABOUT)) desktop.setAboutHandler(null)
            if (onSettings != null && supports(Desktop.Action.APP_PREFERENCES)) desktop.setPreferencesHandler(null)
            if (onQuit != null && supports(Desktop.Action.APP_QUIT_HANDLER)) desktop.setQuitHandler(null)
        }
    }
}

/** A chord as a menu accelerator. Ctrl in a chord is Cmd on macOS, as [KeyChord.caps] shows it. */
internal fun menuShortcut(chord: KeyChord, mac: Boolean = isMac): KeyShortcut? {
    val code = awtKeyCode(chord.key) ?: return null
    return KeyShortcut(
        Key(code),
        ctrl = if (mac) chord.meta else chord.ctrl,
        meta = if (mac) chord.ctrl else chord.meta,
        alt = chord.alt,
        shift = chord.shift,
    )
}

private val namedKeys = mapOf(
    "Esc" to AwtKeyEvent.VK_ESCAPE,
    "Delete" to AwtKeyEvent.VK_DELETE,
    "Backspace" to AwtKeyEvent.VK_BACK_SPACE,
    "Enter" to AwtKeyEvent.VK_ENTER,
    "Space" to AwtKeyEvent.VK_SPACE,
    "Tab" to AwtKeyEvent.VK_TAB,
    "/" to AwtKeyEvent.VK_SLASH,
)

/** Key names as [chord] spells them, back to AWT key codes. */
private val keyCodes: Map<String, Int> by lazy {
    AwtKeyEvent::class.java.fields
        .filter { it.name.startsWith("VK_") && it.type == Int::class.javaPrimitiveType }
        .map { it.getInt(null) }
        .associateBy { AwtKeyEvent.getKeyText(it).let { name -> if (name.length == 1) name.uppercase() else name } }
}

internal fun awtKeyCode(key: String): Int? = namedKeys[key] ?: keyCodes[key]
