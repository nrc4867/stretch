package dev.chieppa.stretch.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.theme.StretchSpacing
import dev.chieppa.stretch.ui.Chip
import dev.chieppa.stretch.ui.ChipKind
import dev.chieppa.stretch.ui.Divider

/** A strip across the window under the top bar, in [dev.chieppa.stretch.frame.StretchApp]'s `topStrip`: a chip per layer. */
@Composable
internal fun LayerStrip(layers: List<String>, selected: String) {
    Column(Modifier.fillMaxWidth().background(Stretch.colors.surfaceRail)) {
        Row(
            Modifier.fillMaxWidth().height(StretchSizes.icon).padding(horizontal = StretchSpacing.space16),
            horizontalArrangement = Arrangement.spacedBy(StretchSpacing.space6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (layer in layers) Chip(layer, kind = if (layer == selected) ChipKind.On else ChipKind.Default, icon = StretchIcons.Layers)
        }
        Divider()
    }
}
