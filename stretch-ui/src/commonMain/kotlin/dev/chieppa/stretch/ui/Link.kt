package dev.chieppa.stretch.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons

/**
 * Underlined text that goes somewhere, for navigation rather than actions, which are [Button]s. [external] marks a
 * link that leaves the app. Opening the destination is [onClick]'s job.
 */
@Composable
fun Link(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, external: Boolean = false) {
    val c = Stretch.colors
    val state = rememberControlState()
    val ink = if (state.hovered || state.pressed) c.inkStrong else c.inkControl
    Row(
        modifier
            .focusRing(state, Stretch.shapes.segShape)
            .pointerHoverIcon(PointerIcon.Hand)
            .hoverable(state.source)
            .clickable(state.source, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(text, Stretch.type.body.copy(textDecoration = TextDecoration.Underline), ink)
        if (external) Icon(StretchIcons.ExternalLink, ink, size = 12.dp)
    }
}
