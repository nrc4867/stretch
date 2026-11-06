package dev.chieppa.stretch.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.Label
import dev.chieppa.stretch.ui.Tree
import dev.chieppa.stretch.ui.TreeNode
import dev.chieppa.stretch.ui.rememberTreeState
import dev.chieppa.stretch.ui.treeLeaves

/** A large folder tree in a [dev.chieppa.stretch.ui.TreeState]: checkboxes that cascade, counts after each folder, and type-to-find. */
@Composable
internal fun CheckedTreeDemo() {
    val roots = remember {
        listOf("assets", "docs", "scripts", "source", "tests").map { top ->
            TreeNode(top, (1..40).map { folder ->
                TreeNode("$top/part $folder", (1..25).map { TreeNode("$top/part $folder/sheet $it.txt") })
            })
        }
    }
    val state = rememberTreeState(expanded = setOf("source", "source/part 1"), checked = setOf("source/part 1/sheet 2.txt"))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label("${state.checked.size} checked, ${state.selection.size} selected", Stretch.type.bodySm, Stretch.colors.inkMuted)
        Tree(
            roots, state, { it }, { it.substringAfterLast('/') },
            Modifier.width(320.dp).height(360.dp),
            icon = { if (it.endsWith(".txt")) StretchIcons.File else StretchIcons.Folder },
            checkboxes = true,
            trailing = { path ->
                val node = remember(path) { findNode(roots, path) }
                if (node != null && node.children.isNotEmpty()) {
                    Label(treeLeaves(node) { it }.size.toString(), Stretch.type.valueSm, Stretch.colors.inkMuted)
                }
            },
        )
    }
}

private fun findNode(nodes: List<TreeNode<String>>, item: String): TreeNode<String>? {
    for (node in nodes) {
        if (node.item == item) return node
        if (item.startsWith(node.item + "/")) return findNode(node.children, item)
    }
    return null
}
