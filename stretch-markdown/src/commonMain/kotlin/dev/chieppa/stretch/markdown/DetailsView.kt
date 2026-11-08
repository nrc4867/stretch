package dev.chieppa.stretch.markdown

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.handPointer

/** The size of the arrow before a summary. */
private val arrowSize = 12.dp

/** `<details>`: closed at first, with an arrow before its summary; a click on the summary opens or closes it. */
@Composable
internal fun DetailsView(details: ViewNode.Details, context: BlockContext) {
    var open by rememberSaveable { mutableStateOf(false) }
    val c = Stretch.colors
    val summary = remember(details, context.onLinkClick) {
        details.summary.text.takeIf { it.isNotBlank() }?.let { clickableLinks(it, context.onLinkClick) } ?: AnnotatedString(context.strings.details)
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(blockGap)) {
        DisableSelection {
            Row(
                Modifier
                    .fillMaxWidth()
                    .handPointer()
                    .clickable(role = Role.Button) { open = !open }
                    .semantics { stateDescription = if (open) context.strings.open else context.strings.closed },
                horizontalArrangement = Arrangement.spacedBy(blockGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (open) StretchIcons.ChevronDown else StretchIcons.ChevronRight, c.inkMuted, size = arrowSize)
                BasicText(summary, Modifier.weight(1f), bodyStyle)
            }
        }
        if (open) ViewNodes(details.children, context, Modifier.padding(start = listGutter))
    }
}
