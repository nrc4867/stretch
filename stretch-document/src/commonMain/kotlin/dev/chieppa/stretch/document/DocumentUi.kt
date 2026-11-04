package dev.chieppa.stretch.document

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.document.generated.Res
import dev.chieppa.stretch.document.generated.cancel
import dev.chieppa.stretch.document.generated.discard
import dev.chieppa.stretch.document.generated.discard_all
import dev.chieppa.stretch.document.generated.dont_save
import dev.chieppa.stretch.document.generated.recent_clear
import dev.chieppa.stretch.document.generated.recent_files
import dev.chieppa.stretch.document.generated.recent_none
import dev.chieppa.stretch.document.generated.recover_body
import dev.chieppa.stretch.document.generated.recover_never_saved
import dev.chieppa.stretch.document.generated.recover_title
import dev.chieppa.stretch.document.generated.restore
import dev.chieppa.stretch.document.generated.save
import dev.chieppa.stretch.document.generated.unsaved_body
import dev.chieppa.stretch.document.generated.unsaved_title
import dev.chieppa.stretch.document.generated.untitled
import dev.chieppa.stretch.frame.MenuDivider
import dev.chieppa.stretch.frame.MenuItem
import dev.chieppa.stretch.frame.MenuLabel
import dev.chieppa.stretch.frame.StretchDialog
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.ButtonKind
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** The document's name for the top bar and window title: its file's name, or "Untitled" before it is saved. */
@Composable
fun <D> documentName(session: DocumentSession<D>): String = session.fileName ?: stringResource(Res.string.untitled)

/**
 * Asks whether to save before [DocumentSession.whenSaved] goes on: Save (picking a file with [chooser] if it has
 * none), Don't save, or Cancel. Shows only while something waits on the answer. [onSaveFailed] hears about a save
 * that threw, so the app can say why; the waiting step is then dropped.
 */
@Composable
fun <D> UnsavedChangesDialog(session: DocumentSession<D>, chooser: FileChooser, onSaveFailed: (Throwable) -> Unit = {}) {
    val then = session.pending ?: return
    val scope = rememberCoroutineScope()
    val cancel = { session.pending = null }
    StretchDialog(
        title = stringResource(Res.string.unsaved_title),
        onDismiss = cancel,
        actions = {
            Button(stringResource(Res.string.cancel), cancel, kind = ButtonKind.Quiet)
            Button(stringResource(Res.string.dont_save), {
                session.pending = null
                then()
            })
            Button(stringResource(Res.string.save), {
                scope.launch {
                    val done = runCatching { session.save(chooser) }.onFailure(onSaveFailed).getOrDefault(false)
                    session.pending = null
                    if (done) then()
                }
            }, kind = ButtonKind.Primary)
        },
    ) {
        Body(stringResource(Res.string.unsaved_body, documentName(session)))
    }
}

/** A document found after the app closed without saving it: its old file, if any, and its name. */
class RecoveredDocument(val key: String, val path: String?, val name: String?)

/**
 * Offers the documents found in recovery, newest first, at startup: Restore one to open it with unsaved changes, or
 * discard them. Shows while [items] is not empty; the app removes each one it restores or discards.
 */
@Composable
fun RecoveryDialog(
    items: List<RecoveredDocument>,
    onRestore: (RecoveredDocument) -> Unit,
    onDiscard: (RecoveredDocument) -> Unit,
    onDismiss: () -> Unit,
) {
    if (items.isEmpty()) return
    val c = Stretch.colors
    StretchDialog(
        title = stringResource(Res.string.recover_title),
        onDismiss = onDismiss,
        width = 460.dp,
        actions = { Button(stringResource(Res.string.discard_all), { items.forEach(onDiscard) }, kind = ButtonKind.Quiet) },
    ) {
        Body(stringResource(Res.string.recover_body))
        Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (item in items) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            item.name ?: stringResource(Res.string.untitled),
                            style = Stretch.type.body.merge(TextStyle(color = c.ink)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        BasicText(
                            item.path ?: stringResource(Res.string.recover_never_saved),
                            style = Stretch.type.bodySm.merge(TextStyle(color = c.inkSubtle)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Button(stringResource(Res.string.discard), { onDiscard(item) }, kind = ButtonKind.Quiet)
                    Button(stringResource(Res.string.restore), { onRestore(item) })
                }
            }
        }
    }
}

@Composable
private fun Body(text: String) {
    BasicText(text, Modifier.padding(14.dp), style = Stretch.type.body.merge(TextStyle(color = Stretch.colors.ink)))
}

/**
 * The files opened or saved lately, newest first and at most [limit]. Keeping the list between runs is the app's
 * job: save [entries] with its settings and pass them back in.
 */
@Stable
class RecentFiles(entries: List<String> = emptyList(), val limit: Int = 10) {
    var entries by mutableStateOf(entries.distinct().take(limit))
        private set

    /** Puts [path] first, as it is opened or saved. */
    fun add(path: String) {
        entries = (listOf(path) + entries.filter { it != path }).take(limit)
    }

    fun remove(path: String) {
        entries = entries - path
    }

    fun clear() {
        entries = emptyList()
    }

    /** Drops files that are gone, such as ones deleted or on a drive that is no longer there. */
    suspend fun prune(storage: DocumentStorage) {
        entries = entries.filter { storage.exists(it) }
    }
}

/**
 * The recent files as menu items, for a File menu or a start screen's menu: each opens through [onOpen], and a last
 * item clears the list. [onDone] closes the menu.
 */
@Composable
fun RecentFilesMenuItems(recent: RecentFiles, storage: DocumentStorage, onOpen: (String) -> Unit, onDone: () -> Unit = {}) {
    MenuLabel(stringResource(Res.string.recent_files))
    if (recent.entries.isEmpty()) {
        BasicText(
            stringResource(Res.string.recent_none),
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            style = Stretch.type.body.merge(TextStyle(color = Stretch.colors.inkSubtle)),
        )
        return
    }
    for (path in recent.entries) {
        MenuItem(storage.displayName(path), { onDone(); onOpen(path) })
    }
    MenuDivider()
    MenuItem(stringResource(Res.string.recent_clear), { recent.clear(); onDone() })
}
