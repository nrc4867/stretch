package dev.chieppa.stretch.markdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Label

/** The bullet before an item of `<ul>`. */
private const val BULLET = "•"

/** `<ul>` or `<ol>`: each item behind its bullet or number, as the editor draws a markdown list. */
@Composable
internal fun ItemsView(items: ViewNode.Items, context: BlockContext) {
    val c = Stretch.colors
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(blockGap)) {
        items.items.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.width(listGutter), contentAlignment = Alignment.TopCenter) {
                    Label(if (items.ordered) "${items.start + index}." else BULLET, Stretch.type.body, c.inkMuted)
                }
                ViewNodes(item, context, Modifier.weight(1f))
            }
        }
    }
}
