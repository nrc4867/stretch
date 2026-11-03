package dev.chieppa.stretch.frame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.actual_size
import dev.chieppa.stretch.frame.generated.fit_view
import dev.chieppa.stretch.frame.generated.navigator
import dev.chieppa.stretch.frame.generated.zoom_in
import dev.chieppa.stretch.frame.generated.zoom_out
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.Divider
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.Tooltip
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** The zoom levels the navigator steps through. */
val ZoomSteps = listOf(0.1f, 0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 3f, 4f, 6f, 8f)

/** The next zoom step above (or below, with [up] false) [zoom]. */
fun nextZoom(zoom: Float, up: Boolean): Float =
    if (up) ZoomSteps.firstOrNull { it > zoom + 1e-4f } ?: ZoomSteps.last() else ZoomSteps.lastOrNull { it < zoom - 1e-4f } ?: ZoomSteps.first()

/**
 * The view navigator, top-right of the viewport: zoom out, the zoom readout (click for 100%), zoom in and fit. With
 * [minimap], a small map of the whole scene sits under it with the visible part outlined; [visible] is that part as
 * fractions of the scene, and dragging it reports [onPan] with the new top-left, also as fractions.
 */
@Composable
fun ViewNavigator(
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    onFit: () -> Unit,
    modifier: Modifier = Modifier,
    minimap: (@Composable BoxScope.() -> Unit)? = null,
    visible: Rect = Rect(0f, 0f, 1f, 1f),
    onPan: (Offset) -> Unit = {},
    minimapSize: Dp = 160.dp,
) {
    val c = Stretch.colors
    val shape = Stretch.shapes.cardShape
    Column(modifier.floating(shape, Stretch.shadows.pop)) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            val zoomOut = stringResource(Res.string.zoom_out)
            val zoomIn = stringResource(Res.string.zoom_in)
            val actual = stringResource(Res.string.actual_size)
            val fit = stringResource(Res.string.fit_view)
            Tooltip(zoomOut, listOf("Ctrl", "-")) { IconButton(StretchIcons.Minus, zoomOut, { onZoomChange(nextZoom(zoom, false)) }, size = StretchSizes.seg) }
            Tooltip(actual, listOf("Ctrl", "0")) {
                val source = remember { MutableInteractionSource() }
                val hovered by source.collectIsHoveredAsState()
                val pressed by source.collectIsPressedAsState()
                Box(
                    Modifier
                        .widthIn(min = 48.dp)
                        .height(StretchSizes.seg)
                        .clip(Stretch.shapes.segShape)
                        .background(if (pressed) c.surfaceControlHi else if (hovered) c.surfaceControl else Color.Transparent)
                        .hoverable(source)
                        .clickable(source, null, role = Role.Button) { onZoomChange(1f) }
                        .semantics { contentDescription = actual },
                    contentAlignment = Alignment.Center,
                ) { BasicText("${(zoom * 100).roundToInt()}%", style = Stretch.type.value.merge(TextStyle(color = c.inkStrong))) }
            }
            Tooltip(zoomIn, listOf("Ctrl", "=")) { IconButton(StretchIcons.Plus, zoomIn, { onZoomChange(nextZoom(zoom, true)) }, size = StretchSizes.seg) }
            Tooltip(fit, listOf("Shift", "1")) { IconButton(StretchIcons.Fit, fit, onFit, size = StretchSizes.seg) }
        }
        if (minimap != null) {
            val pan by rememberUpdatedState(onPan)
            val shown by rememberUpdatedState(visible)
            val description = stringResource(Res.string.navigator)
            Divider()
            Box(
                Modifier
                    .padding(6.dp)
                    .size(minimapSize, minimapSize * 0.66f)
                    .clip(Stretch.shapes.segShape)
                    .background(c.surfaceWell)
                    .semantics { contentDescription = description },
            ) {
                minimap()
                Canvas(
                    Modifier.matchParentSize().pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val start = shown.topLeft
                            fun to(p: Offset) {
                                val d = Offset((p.x - down.position.x) / size.width, (p.y - down.position.y) / size.height)
                                val inside = shown.contains(Offset(down.position.x / size.width, down.position.y / size.height))
                                val target = if (inside) start + d else Offset(p.x / size.width - shown.width / 2, p.y / size.height - shown.height / 2)
                                pan(Offset(target.x.coerceIn(0f, 1f - shown.width), target.y.coerceIn(0f, 1f - shown.height)))
                            }
                            to(down.position)
                            drag(down.id) { ch ->
                                ch.consume()
                                to(ch.position)
                            }
                        }
                    },
                ) {
                    val r = Rect(visible.left * size.width, visible.top * size.height, visible.right * size.width, visible.bottom * size.height)
                    drawRect(c.ringSelected, r.topLeft, r.size, style = Stroke(2.dp.toPx()))
                }
            }
        }
    }
}

/**
 * The 24px status bar along the bottom of the window, for quiet facts: background jobs, the selection, the units.
 * Nothing here needs action; anything that does belongs in a callout or a toast.
 */
@Composable
fun StatusBar(modifier: Modifier = Modifier, start: @Composable RowScope.() -> Unit = {}, end: @Composable RowScope.() -> Unit = {}) {
    val c = Stretch.colors
    Column(modifier.fillMaxWidth()) {
        Divider()
        Row(
            Modifier.fillMaxWidth().height(24.dp).background(c.surfaceRail).padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            start()
            Spacer(Modifier.weight(1f))
            end()
        }
    }
}

/** One fact in the [StatusBar]: an optional icon and short text. With [onClick] it opens what it describes. */
@Composable
fun StatusItem(text: String, icon: ImageVector? = null, onClick: (() -> Unit)? = null) {
    val c = Stretch.colors
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxHeight()
            .hoverable(source)
            .then(if (onClick != null) Modifier.clickable(source, null, role = Role.Button, onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ink = if (hovered && onClick != null) c.inkStrong else c.inkMuted
        if (icon != null) Icon(icon, ink, size = 12.dp)
        BasicText(text, style = Stretch.type.valueSm.merge(TextStyle(color = ink)), maxLines = 1)
    }
}
