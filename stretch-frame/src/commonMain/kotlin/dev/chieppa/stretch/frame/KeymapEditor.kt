package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.core.Action
import dev.chieppa.stretch.core.KeyChord
import dev.chieppa.stretch.core.Keymap
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.keymap_conflict
import dev.chieppa.stretch.frame.generated.keymap_reset
import dev.chieppa.stretch.frame.generated.keymap_search
import dev.chieppa.stretch.frame.generated.keymap_shortcut
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.TextField
import dev.chieppa.stretch.ui.Tooltip
import org.jetbrains.compose.resources.stringResource

/**
 * Lists [actions] with their shortcuts for people to change: search by name, click a shortcut and press new keys,
 * reset a changed one to its default. A shortcut that another action active at the same time also uses gets a
 * warning naming that action. Changes go straight into [keymap]; [onChange] tells the app, which saves
 * [Keymap.changes] where it keeps settings. Opt in by putting it in a drawer, dialog or settings page.
 */
@Composable
fun KeymapEditor(keymap: Keymap, actions: List<Action>, modifier: Modifier = Modifier, onChange: () -> Unit = {}) {
    val text = LocalActionText.current
    var query by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    fun changed() {
        revision++
        onChange()
    }
    val names = actions.associate { it.id to text(it.textKey) }
    // Read so the list redraws after a change; Keymap itself is plain state.
    val conflicts = revision.let { keymap.conflicts() }
    val shown = actions.filter { query.isBlank() || names.getValue(it.id).contains(query.trim(), ignoreCase = true) }
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TextField(query, { query = it }, placeholder = stringResource(Res.string.keymap_search), icon = StretchIcons.Search)
        for (action in shown) {
            val name = names.getValue(action.id)
            val keys = keymap.keysFor(action.id)
            val clash = conflicts.firstOrNull { it.first == action.id || it.second == action.id }
                ?.let { if (it.first == action.id) it.second else it.first }
            KeymapRow(
                name = name,
                keys = keys,
                changed = action.id in keymap.changes,
                clashWith = clash?.let { id -> names[id] ?: id.value },
                onKeys = { keymap.rebind(action.id, it); changed() },
                onReset = { keymap.reset(action.id); changed() },
            )
        }
    }
}

@Composable
private fun KeymapRow(
    name: String,
    keys: List<KeyChord>,
    changed: Boolean,
    clashWith: String?,
    onKeys: (List<KeyChord>) -> Unit,
    onReset: () -> Unit,
) {
    val c = Stretch.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = StretchSizes.ctl),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            name,
            Modifier.weight(1f),
            style = Stretch.type.body.merge(TextStyle(color = c.inkControl)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (clashWith != null) {
            val message = stringResource(Res.string.keymap_conflict, clashWith)
            Tooltip(message) { Icon(StretchIcons.Warn, c.warnInk, contentDescription = message) }
        }
        ShortcutField(
            chord = keys.firstOrNull(),
            onChordChange = { chord -> onKeys(listOfNotNull(chord) + keys.drop(1)) },
            contentDescription = stringResource(Res.string.keymap_shortcut, name),
            modifier = Modifier.width(150.dp),
        )
        if (changed) {
            val reset = stringResource(Res.string.keymap_reset)
            Tooltip(reset) { IconButton(StretchIcons.Undo, reset, onReset, size = StretchSizes.seg) }
        } else {
            Spacer(Modifier.size(StretchSizes.seg))
        }
    }
}
