package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.close
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.Divider
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.ScrollColumn
import dev.chieppa.stretch.ui.StretchPopup
import dev.chieppa.stretch.ui.TransientPopup
import org.jetbrains.compose.resources.stringResource

/** The padding around a popover's content and footer, and the space between their children. */
private val PopoverPadding = 12.dp
private val PopoverSpacing = 8.dp

/**
 * A popover: a small panel of live controls anchored to the thing that opened it, like a brush's settings or a
 * color picker. Unlike a [Menu] it stays open while you work in it; a click outside or Esc closes it. Changes in it
 * apply at once, so it has no OK button. With [anchor], a rectangle in pixels from the top left of the element the
 * popover is composed in, it opens under that rectangle instead, such as under a name a code view reports clicked.
 *
 * A popover never grows past [maxHeight], nor past the room beside its anchor (above it when there is more room there),
 * as a [Menu] does. The title and the [footer], such as a row with the panel's main button, stay put; when everything
 * does not fit, only [content] scrolls, with Stretch's scrollbar, and a focused control in it scrolls into view. A
 * popover that fits shows its full height. It shows only once it knows that room, so it appears at its final size
 * and place.
 */
@Composable
fun Popover(
    open: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    width: Dp = 280.dp,
    anchor: Rect? = null,
    maxHeight: Dp? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!open) return
    var room by remember { mutableIntStateOf(Int.MAX_VALUE) }
    val density = LocalDensity.current
    val position = remember(anchor) { BelowAnchor(6, anchor?.roundToIntRect(), onRoom = { if (it != room) room = it }) }
    // Read here rather than while drawing: the placement that tells the room comes after this composition.
    val placed = room != Int.MAX_VALUE
    val cap = if (placed) overlayHeightCap(with(density) { room.toDp() }, maxHeight) else Dp.Unspecified
    StretchPopup(popupPositionProvider = position, onDismissRequest = onDismiss, properties = TransientPopup, shadow = Stretch.shadows.pop) {
        Box(if (placed) Modifier else Modifier.drawWithContent {}) {
            Column(
                Modifier
                    .width(width)
                    .then(if (cap == Dp.Unspecified) Modifier else Modifier.heightIn(max = cap))
                    .floating(Stretch.shapes.cardShape, Stretch.shadows.pop)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                            onDismiss()
                            true
                        } else {
                            false
                        }
                    },
            ) {
                if (title != null) PopoverTitle(title, onDismiss)
                ScrollColumn(Modifier.weight(1f, fill = false)) {
                    Column(Modifier.padding(PopoverPadding), verticalArrangement = Arrangement.spacedBy(PopoverSpacing), content = content)
                }
                if (footer != null) {
                    Divider()
                    Column(
                        Modifier.fillMaxWidth().padding(PopoverPadding),
                        verticalArrangement = Arrangement.spacedBy(PopoverSpacing),
                        content = footer,
                    )
                }
            }
        }
    }
}

/** A popover's header: its title and a close button, over a line. */
@Composable
private fun PopoverTitle(title: String, onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(StretchSizes.row).padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(title, Modifier.weight(1f), style = Stretch.type.body.merge(TextStyle(color = Stretch.colors.inkStrong)), maxLines = 1)
        IconButton(StretchIcons.Close, stringResource(Res.string.close), onDismiss, size = StretchSizes.seg)
    }
    Divider()
}
