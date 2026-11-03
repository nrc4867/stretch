package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

/** A line across the width it is given, as heavy as the theme's panel borders. [label] sits in a gap in the middle. */
@Composable
fun Divider(modifier: Modifier = Modifier, label: String? = null) {
    val c = Stretch.colors
    if (label == null) {
        Box(modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
    } else {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(Stretch.stroke).background(c.linePanel))
            Label(label, Stretch.type.bodySm, c.inkMuted)
            Box(Modifier.weight(1f).height(Stretch.stroke).background(c.linePanel))
        }
    }
}

/** A [Divider] standing up, for groups in a row. It fills the height it is given, so give it a height. */
@Composable
fun VerticalDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxHeight().width(Stretch.stroke).background(Stretch.colors.linePanel))
}
