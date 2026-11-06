package dev.chieppa.stretch.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.ContextMenuPopup
import dev.chieppa.stretch.frame.MenuItem
import dev.chieppa.stretch.frame.PaneDropTarget
import dev.chieppa.stretch.frame.TabItem
import dev.chieppa.stretch.frame.paneDragSource
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.Tree
import dev.chieppa.stretch.ui.TreeNode
import dev.chieppa.stretch.ui.rememberTreeState

/** Views not open yet, in a tree: drag one into the panes, or open one from its context menu. */
@Composable
internal fun ViewShelf(target: PaneDropTarget, tab: (String) -> TabItem, onOpen: (id: String, aside: Boolean) -> Unit) {
    val roots = remember { listOf(TreeNode("shelf", shelfViews.map { TreeNode(it) })) }
    val state = rememberTreeState(expanded = setOf("shelf"))
    var menu by remember { mutableStateOf<Pair<String, Offset>?>(null) }
    Box(Modifier.width(180.dp).fillMaxHeight()) {
        Tree(
            roots, state, { it }, { if (it == "shelf") "More views" else tab(it).title },
            icon = { if (it == "shelf") StretchIcons.Folder else tab(it).icon },
            onActivate = { if (it != "shelf") onOpen(it, false) },
            onContextMenu = { item, at -> if (item != "shelf") menu = item to at },
            rowModifier = { item -> if (item == "shelf") Modifier else Modifier.paneDragSource(target) { tab(item) } },
        )
        ContextMenuPopup(menu?.second, { menu = null }) { close ->
            val id = menu?.first ?: return@ContextMenuPopup
            MenuItem("Open", { close(); onOpen(id, false) })
            MenuItem("Open to the side", { close(); onOpen(id, true) })
        }
    }
}

/** The ids of the views the shelf offers. */
internal val shelfViews = listOf("palette", "brushes", "timeline")
