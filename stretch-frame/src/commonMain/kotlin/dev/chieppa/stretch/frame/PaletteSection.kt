package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSpacing

/** The eyebrow over a group of [SearchPalette] results, with an optional quiet [note] at its end. */
@Composable
fun PaletteSection(title: String, modifier: Modifier = Modifier, note: String? = null) {
    val c = Stretch.colors
    Row(
        modifier.fillMaxWidth().padding(start = StretchSpacing.space10, end = StretchSpacing.space10, top = StretchSpacing.space8, bottom = StretchSpacing.space4),
        horizontalArrangement = Arrangement.spacedBy(StretchSpacing.space6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(title.uppercase(), Modifier.semantics { heading() }, style = Stretch.type.eyebrow.merge(TextStyle(color = c.inkMuted)), maxLines = 1)
        Spacer(Modifier.weight(1f))
        if (note != null) BasicText(note, style = Stretch.type.bodySm.merge(TextStyle(color = c.inkSubtle, fontSize = Stretch.type.value.fontSize)), maxLines = 1)
    }
}
