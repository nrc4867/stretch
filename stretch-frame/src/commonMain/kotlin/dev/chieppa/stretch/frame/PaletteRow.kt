package dev.chieppa.stretch.frame

import dev.chieppa.stretch.ui.controlPointer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSpacing

private val PALETTE_ROW_HEIGHT = 30.dp

/**
 * One result in a [SearchPalette]: a row at least 30dp tall laid out by [content], usually an icon, a label and quieter
 * details after it. The [highlighted] row is the one Enter picks, and scrolls into view when it becomes so.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaletteRow(highlighted: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val c = Stretch.colors
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val reveal = remember { BringIntoViewRequester() }
    LaunchedEffect(highlighted) { if (highlighted) reveal.bringIntoView() }
    Row(
        modifier
            .bringIntoViewRequester(reveal)
            .fillMaxWidth()
            .heightIn(min = PALETTE_ROW_HEIGHT)
            .clip(Stretch.shapes.segShape)
            .background(if (pressed) c.surfaceControlPress else if (highlighted || hovered) c.surfaceControlHi else Color.Transparent)
            .hoverable(source)
            .controlPointer().clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { selected = highlighted }
            .padding(horizontal = StretchSpacing.space10),
        horizontalArrangement = Arrangement.spacedBy(StretchSpacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
