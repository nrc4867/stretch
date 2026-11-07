package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.chieppa.stretch.core.KeyChord
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.commands
import dev.chieppa.stretch.frame.generated.redo
import dev.chieppa.stretch.frame.generated.settings
import dev.chieppa.stretch.frame.generated.undo
import dev.chieppa.stretch.theme.StretchIcons
import org.jetbrains.compose.resources.stringResource

/**
 * One window-level action for [TopBarActions]: shown as an icon button with a tooltip, or as a menu item once it
 * folds into the More menu. [popup], such as a [Popover], opens from wherever the action is showing; pass whether it
 * is open as [popupOpen] and the action's button opens it on press. [badge] puts a count on the button, such as
 * `badgeCount(unread)`, or a dot for an empty string; null shows none.
 */
class BarAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val keys: List<String> = emptyList(),
    val enabled: Boolean = true,
    val popupOpen: Boolean? = null,
    val popup: @Composable () -> Unit = {},
    val badge: String? = null,
)

/**
 * The standard top bar actions: the app's [leading] ones, such as a notifications bell, then undo, redo, the command
 * palette and settings, then the app's [more]. Pass null to leave one out. Put what settings opens, such as a
 * [Popover], in [settingsPopup], so it anchors under the settings button, and whether it is open in [settingsOpen]. [undoEnabled] and [redoEnabled] keep their buttons in place, faint,
 * while there is nothing to take back or bring back. [commandsKeys] are the key caps the command palette's tooltip and
 * menu item show, such as `keymap.capsFor(action)` for the app's own binding; Ctrl K (Cmd K on macOS) unless given,
 * none when null. Actions that do not fit fold, last first, into a More menu at the end.
 */
@Composable
fun TopBarActions(
    onUndo: (() -> Unit)?,
    onRedo: (() -> Unit)?,
    onCommands: (() -> Unit)?,
    onSettings: (() -> Unit)? = null,
    more: List<BarAction> = emptyList(),
    leading: List<BarAction> = emptyList(),
    undoEnabled: Boolean = true,
    redoEnabled: Boolean = true,
    settingsOpen: Boolean? = null,
    commandsKeys: List<String>? = DEFAULT_COMMANDS_KEYS.caps(),
    settingsPopup: @Composable () -> Unit = {},
) {
    val undo = stringResource(Res.string.undo)
    val redo = stringResource(Res.string.redo)
    val commands = stringResource(Res.string.commands)
    val settings = stringResource(Res.string.settings)
    val actions = leading + listOfNotNull(
        onUndo?.let { BarAction(StretchIcons.Undo, undo, it, listOf("Ctrl", "Z"), enabled = undoEnabled) },
        onRedo?.let { BarAction(StretchIcons.Redo, redo, it, listOf("Ctrl", "Shift", "Z"), enabled = redoEnabled) },
        onCommands?.let { BarAction(StretchIcons.Command, commands, it, commandsKeys.orEmpty()) },
        onSettings?.let { BarAction(StretchIcons.Settings, settings, it, popup = settingsPopup, popupOpen = settingsOpen) },
    ) + more
    OverflowActions(actions)
}

/** The command palette's usual binding. */
private val DEFAULT_COMMANDS_KEYS = KeyChord.parse("Ctrl K")
