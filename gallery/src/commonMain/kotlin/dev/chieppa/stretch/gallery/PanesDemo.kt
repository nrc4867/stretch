package dev.chieppa.stretch.gallery

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.MenuItem
import dev.chieppa.stretch.frame.PaneDrop
import dev.chieppa.stretch.frame.PaneEdge
import dev.chieppa.stretch.frame.PaneGrid
import dev.chieppa.stretch.frame.PaneItemDrop
import dev.chieppa.stretch.frame.PaneLayout
import dev.chieppa.stretch.frame.RowText
import dev.chieppa.stretch.frame.TabItem
import dev.chieppa.stretch.frame.place
import dev.chieppa.stretch.frame.rememberPaneDropTarget
import dev.chieppa.stretch.frame.returnWindow
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.Tooltip
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.Toggle

private val sampleViews = mapOf(
    "outline" to ("Outline" to StretchIcons.ListBullet),
    "sketch" to ("Sketch" to StretchIcons.Pen),
    "notes" to ("Notes" to StretchIcons.File),
    "board" to ("Board" to StretchIcons.Grid),
    "palette" to ("Palette" to StretchIcons.Eyedropper),
    "brushes" to ("Brushes" to StretchIcons.Pen),
    "timeline" to ("Timeline" to StretchIcons.Keyframe),
)

/** Views that say which document they belong to, shown as their tab's subtitle. */
private val sampleDocuments = mapOf("notes" to "draft.md", "board" to "plan.board")

private fun sampleTab(id: String, modified: Boolean): TabItem {
    val (title, icon) = sampleViews[id] ?: ("View ${id.removePrefix("view-")}" to StretchIcons.Layers)
    return TabItem(id, title, icon, modified = modified, subtitle = sampleDocuments[id])
}

/**
 * Panes of tabs: drag a tab along a strip, into another pane, or onto a pane's edge to split it; drag a border to
 * resize. On the desktop a tab let go away from the panes, or sent by its menu, opens in a window of its own. Views
 * on the shelf drag in the same way, and each strip ends with a button that splits off its showing tab.
 */
@Composable
internal fun PanesDemo() {
    var layout by remember {
        mutableStateOf(
            PaneLayout().open("outline").open("sketch").open("notes").open("board")
                .splitWith("notes", PaneLayout.FIRST_LEAF, PaneEdge.End)
                .activate("outline"),
        )
    }
    var modified by remember { mutableStateOf(setOf("sketch")) }
    var opened by remember { mutableIntStateOf(0) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button("Open a view", { opened++; layout = layout.open("view-$opened") }, icon = StretchIcons.Plus)
        if (layout.windows.isNotEmpty()) {
            Button("Bring windows back", { layout = layout.windows.fold(layout) { l, w -> l.returnWindow(w.id) } }, icon = StretchIcons.Restore)
        }
    }
    val target = rememberPaneDropTarget()
    Row(Modifier.fillMaxWidth().height(360.dp).border(Stretch.stroke, Stretch.colors.linePanel)) {
        ViewShelf(target, { sampleTab(it, false) }) { id, aside ->
            layout = if (aside) layout.place(id, PaneItemDrop.InPane(PaneDrop.Edge(layout.focused, PaneEdge.End))) else layout.open(id)
        }
        PaneGrid(
            layout = layout,
            onChange = { layout = it },
            item = { sampleTab(it, it in modified) },
            modifier = Modifier.weight(1f),
            dropTarget = target,
            paneActions = { pane ->
                val leaf = layout.leaf(pane)
                val shown = leaf?.active
                if (shown != null && leaf.tabs.size > 1) {
                    Tooltip("Split off $shown") {
                        IconButton(StretchIcons.SplitView, "Split off $shown", { layout = layout.splitWith(shown, pane, PaneEdge.End) }, size = StretchSizes.seg)
                    }
                }
            },
            tabMenu = { id, close ->
                MenuItem(if (id in modified) "Mark saved" else "Mark modified", { modified = if (id in modified) modified - id else modified + id; close() })
            },
        ) { id ->
            var count by rememberSaveable { mutableIntStateOf(0) }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RowText("${sampleTab(id, false).title}: clicked $count times. The count stays while another tab shows.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button("Click", { count++ })
                    Toggle(id in modified, { modified = if (it) modified + id else modified - id }, "Modified")
                }
            }
        }
    }
}
