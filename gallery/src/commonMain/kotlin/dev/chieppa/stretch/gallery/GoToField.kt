package dev.chieppa.stretch.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.theme.StretchSpacing

private val GO_TO_WIDTH = 280.dp

/** A search-looking button for the top bar's centre that opens the command palette. */
@Composable
internal fun GoToField(onClick: () -> Unit) {
    val c = Stretch.colors
    val shape = Stretch.shapes.ctlShape
    Row(
        Modifier
            .width(GO_TO_WIDTH)
            .height(StretchSizes.seg)
            .clip(shape)
            .background(c.surfaceWell)
            .border(Stretch.stroke, c.lineControl, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = StretchSpacing.space8),
        horizontalArrangement = Arrangement.spacedBy(StretchSpacing.space6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(StretchIcons.Search, c.inkMuted)
        BasicText("Go to…", Modifier.weight(1f), style = Stretch.type.bodySm.merge(TextStyle(color = c.inkSubtle)), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
