package dev.chieppa.stretch.gallery

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.chieppa.stretch.frame.DrawerSpec
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.Tree
import dev.chieppa.stretch.ui.TreeNode
import dev.chieppa.stretch.ui.rememberTreeState

private const val NOTE_FOLDERS = 200
private const val NOTES_PER_FOLDER = 50

/**
 * A drawer that scrolls itself: a tree of ten thousand notes that builds only the rows on screen, docked or in its own
 * window.
 */
internal fun notesDrawer() = DrawerSpec("notes", "Notes", StretchIcons.ListBullet, scrolls = false) {
    val roots = remember {
        (1..NOTE_FOLDERS).map { folder ->
            TreeNode("Folder $folder", (1..NOTES_PER_FOLDER).map { TreeNode("Folder $folder/Note $it") })
        }
    }
    val state = rememberTreeState(expanded = setOf("Folder 1"))
    Tree(
        roots, state, { it }, { it.substringAfterLast('/') },
        Modifier.fillMaxWidth().weight(1f),
        icon = { if ('/' in it) StretchIcons.File else StretchIcons.Folder },
    )
}
