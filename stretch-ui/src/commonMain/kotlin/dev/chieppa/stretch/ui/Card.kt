package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons

private val selectedRingWidth = 2.dp
private val selectedRingGap = 1.dp
private val artCardMinHeight = 120.dp
private val addCardMinHeight = 64.dp
private val captionMinHeight = 36.dp
private val cardBadgeInset = 8.dp
private val cardBadgeDot = 12.dp

/** How much an [ArtCard]'s caption says. The card's own measurements come from its modifier. */
enum class CardSize {
    /** One line: title, then the detail at the far end. */
    Regular,

    /** A heading with the detail under it, for the one card a screen leads with. */
    Large,
}

/**
 * A panel with the theme's card corners and border. It takes its size from [modifier], so one screen can mix cards of
 * any size. With [onClick] the whole card is a control. With [selected] it is one choice of several, and a selected
 * card wears a ring outside its border.
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardSurface(modifier, onClick, selected, Stretch.colors.linePanel, dashed = false, content)
}

/**
 * A card that shows a picture of something, with a caption under it: a shot, a character, a project. [art] fills what
 * the caption leaves and is clipped to it. Give the card its height through [modifier], since it takes all the height
 * on offer; in a scrolling column, where none is on offer, it keeps a small minimum. [trailing] holds a chip or an
 * action, such as the button on a [CardSize.Large] card. [badge] puts a dot on the art's corner and names it for
 * screen readers.
 */
@Composable
fun ArtCard(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    size: CardSize = CardSize.Regular,
    selected: Boolean? = null,
    badge: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    art: @Composable BoxScope.() -> Unit,
) {
    val c = Stretch.colors
    Card(modifier.defaultMinSize(minHeight = artCardMinHeight), onClick, selected) {
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().background(c.surfaceWell)) {
            art()
            if (badge != null) BadgeMark("", badge, Modifier.align(Alignment.TopEnd).padding(cardBadgeInset), dot = cardBadgeDot)
        }
        Divider()
        when (size) {
            CardSize.Regular -> RegularCaption(title, detail, trailing)
            CardSize.Large -> LargeCaption(title, detail, trailing)
        }
    }
}

/** A dashed card that starts something new: an icon over a label. It sits among [ArtCard]s and holds no art. */
@Composable
fun AddCard(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector = StretchIcons.Plus) {
    val c = Stretch.colors
    CardSurface(modifier.defaultMinSize(minHeight = addCardMinHeight), onClick, null, c.chipEdge, dashed = true) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(iconGap, Alignment.CenterVertically),
        ) {
            Icon(icon, c.inkControl)
            Label(label, Stretch.type.body, c.inkControl)
        }
    }
}

@Composable
private fun CardSurface(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    selected: Boolean?,
    edge: Color,
    dashed: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Stretch.colors
    val shape = Stretch.shapes.cardShape
    val radius = Stretch.shapes.card
    val control = rememberControlState()
    val press = when {
        onClick == null -> Modifier
        selected != null -> Modifier.hoverable(control.source).controlPointer().selectable(selected, control.source, null, role = Role.Button, onClick = onClick)
        else -> Modifier.hoverable(control.source).controlPointer().clickable(control.source, null, role = Role.Button, onClick = onClick)
    }
    Column(
        modifier
            .then(if (selected == true) Modifier.outerRing(c.ringSelected, selectedRingWidth, selectedRingGap, radius) else Modifier)
            .focusRing(control, shape)
            .clip(shape)
            .background(control.fill(c.surfacePanel, c.surfaceControl, c.surfaceControlHi))
            .then(if (dashed) Modifier.dashedBorder(edge, Stretch.stroke, radius, dashed = true) else Modifier.border(Stretch.stroke, edge, shape))
            .then(press),
        content = content,
    )
}

@Composable
private fun RegularCaption(title: String, detail: String?, trailing: (@Composable RowScope.() -> Unit)?) {
    val c = Stretch.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = captionMinHeight).padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(title, Stretch.type.body.copy(fontWeight = FontWeight.Medium), c.ink, Modifier.weight(1f))
        trailing?.invoke(this)
        if (detail != null) Label(detail, Stretch.type.value, c.inkMuted)
    }
}

@Composable
private fun LargeCaption(title: String, detail: String?, trailing: (@Composable RowScope.() -> Unit)?) {
    val c = Stretch.colors
    Row(
        Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Label(title, Stretch.type.heading, c.inkStrong)
            if (detail != null) Label(detail, Stretch.type.value, c.inkMuted)
        }
        trailing?.invoke(this)
    }
}

/** A ring [gap] outside this element's bounds. Put it before any clip. */
private fun Modifier.outerRing(color: Color, width: Dp, gap: Dp, radius: Dp) = drawBehind {
    val out = gap.toPx() + width.toPx() / 2
    drawRoundRect(
        color,
        topLeft = Offset(-out, -out),
        size = Size(size.width + 2 * out, size.height + 2 * out),
        cornerRadius = CornerRadius(radius.toPx() + out),
        style = Stroke(width.toPx()),
    )
}
