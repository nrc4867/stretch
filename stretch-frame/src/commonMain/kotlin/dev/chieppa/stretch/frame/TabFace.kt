package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.theme.StretchSpacing
import dev.chieppa.stretch.ui.FieldIcon
import dev.chieppa.stretch.ui.Tooltip

/**
 * One tab in a [TabStrip]: its [title], an optional leading [icon], and whether it has [modified] content, shown as a
 * dot that turns into the close button under the pointer. A tab not [closable] has no close button. A [subtitle], such
 * as the project a file is from, shows quieter after the title and is cut short before it; screen readers hear both,
 * and a tooltip shows them whole when either is cut short.
 */
data class TabItem(
    val id: String,
    val title: String,
    val icon: ImageVector? = null,
    val modified: Boolean = false,
    val closable: Boolean = true,
    val subtitle: String? = null,
)

/**
 * How a tab looks: `ink-strong` on the panel when [active], with a bar under it in the accent while its strip is
 * [focused] and in a strong line otherwise; `ink-muted` on the strip when not. [onClose] makes the close button.
 */
@Composable
internal fun TabFace(
    item: TabItem,
    active: Boolean,
    focused: Boolean,
    hovered: Boolean,
    closeDescription: String,
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var cut by remember { mutableStateOf(false) }
    val face = @Composable { TabFaceBody(item, active, focused, hovered, closeDescription, onClose, modifier) { cut = it } }
    if (cut) Tooltip(listOfNotNull(item.title, item.subtitle).joinToString(TITLE_SEPARATOR)) { face() } else face()
}

@Composable
private fun TabFaceBody(
    item: TabItem,
    active: Boolean,
    focused: Boolean,
    hovered: Boolean,
    closeDescription: String,
    onClose: (() -> Unit)?,
    modifier: Modifier,
    onCut: (Boolean) -> Unit,
) {
    val c = Stretch.colors
    val ink = if (active) c.inkStrong else c.inkMuted
    Column(modifier.height(StretchSizes.row).widthIn(TabMinWidth, TabMaxWidth).width(IntrinsicSize.Max).background(if (active) c.surfacePanel else Color.Transparent)) {
        Row(
            Modifier.weight(1f).padding(start = StretchSpacing.space12, end = if (onClose != null) StretchSpacing.space4 else StretchSpacing.space12),
            horizontalArrangement = Arrangement.spacedBy(StretchSpacing.space6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.icon != null) Icon(item.icon, if (active) c.inkControl else c.inkMuted)
            TabTitle(item.title, item.subtitle, ink, if (active) c.inkMuted else c.inkSubtle, Modifier.weight(1f, fill = false), onCut)
            if (onClose != null && (!item.modified || hovered)) {
                FieldIcon(StretchIcons.Close, contentDescription = closeDescription, onClick = onClose)
            } else if (item.modified) {
                Box(Modifier.size(StretchSizes.chip), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(StretchSpacing.space8).background(c.inkControl, CircleShape))
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(StretchSpacing.space2)
                .background(if (!active) Color.Transparent else if (focused) c.accent else c.lineStrong),
        )
    }
}

/** Between a tab's title and subtitle in its tooltip. */
private const val TITLE_SEPARATOR = " · "
