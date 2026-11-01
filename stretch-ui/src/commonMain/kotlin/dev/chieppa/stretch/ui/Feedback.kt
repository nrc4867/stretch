package dev.chieppa.stretch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons

/**
 * A problem that needs a decision: a label, the measured value and a fix. Lives in the drawer that owns the problem.
 */
@Composable
fun Callout(
    text: String,
    fixLabel: String,
    onFix: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
) {
    val c = Stretch.colors
    val shape = Stretch.shapes.ctlShape
    Row(
        modifier = modifier
            .clip(shape)
            .background(c.warnWash)
            .border(Stretch.stroke, c.warnEdge, shape)
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(StretchIcons.Warn, c.warnInk)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Label(text, Stretch.type.body.copy(fontWeight = FontWeight.Medium), c.warnInk, Modifier.weight(1f, fill = false))
            if (value != null) Label("· $value", Stretch.type.value, c.warnInk)
        }
        Button(fixLabel, onFix)
    }
}

/** An inline progress bar for background work, placed where the change was made. [fraction] is 0..1. */
@Composable
fun ProgressBar(fraction: Float, contentDescription: String, modifier: Modifier = Modifier) {
    val c = Stretch.colors
    Canvas(
        modifier
            .widthIn(min = 80.dp)
            .fillMaxWidth()
            .height(8.dp)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(fraction.coerceIn(0f, 1f), 0f..1f)
            },
    ) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(c.track, cornerRadius = radius)
        drawRoundRect(c.accent, size = Size(size.width * fraction.coerceIn(0f, 1f), size.height), cornerRadius = radius)
        drawRoundRect(c.trackEdge, Offset(0.5f, 0.5f), Size(size.width - 1f, size.height - 1f), radius, style = Stroke(1f))
    }
}
