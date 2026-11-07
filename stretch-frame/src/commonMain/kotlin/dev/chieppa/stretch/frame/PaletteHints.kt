package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSpacing
import dev.chieppa.stretch.ui.Divider
import dev.chieppa.stretch.ui.Kbd

/** One key hint in a palette's footer: the key caps, such as `listOf("Ctrl", "↵")`, and what they do. */
data class PaletteHint(val keys: List<String>, val label: String)

/** A palette's footer: what its keys do, in a row on the panel color under a rule. */
@Composable
fun PaletteHints(hints: List<PaletteHint>, modifier: Modifier = Modifier) {
    val c = Stretch.colors
    Column(modifier.fillMaxWidth()) {
        Divider()
        Row(
            Modifier.fillMaxWidth().background(c.surfacePanel).padding(horizontal = StretchSpacing.space14, vertical = StretchSpacing.space8),
            horizontalArrangement = Arrangement.spacedBy(StretchSpacing.space16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (hint in hints) {
                Row(horizontalArrangement = Arrangement.spacedBy(StretchSpacing.space6), verticalAlignment = Alignment.CenterVertically) {
                    Kbd(hint.keys)
                    BasicText(hint.label, style = Stretch.type.bodySm.merge(TextStyle(color = c.inkMuted, fontSize = Stretch.type.value.fontSize)), maxLines = 1)
                }
            }
        }
    }
}
