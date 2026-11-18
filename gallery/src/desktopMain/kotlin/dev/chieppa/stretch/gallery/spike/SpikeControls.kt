package dev.chieppa.stretch.gallery.spike

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Segmented
import dev.chieppa.stretch.ui.Toggle

/** The spike's settings and its live readings. In the heavyweight run the readback choices are off. */
@Composable
fun SpikeControls(
    heavy: Boolean,
    mode: SpikeMode,
    onMode: (SpikeMode) -> Unit,
    resolution: SpikeResolution,
    onResolution: (SpikeResolution) -> Unit,
    spinning: Boolean,
    onSpinning: (Boolean) -> Unit,
    sway: Boolean,
    onSway: (Boolean) -> Unit,
    header: String,
    readings: List<String>,
    modifier: Modifier,
) {
    val c = Stretch.colors
    val label = Stretch.type.body.merge(TextStyle(color = c.inkStrong))
    val small = Stretch.type.bodySm.merge(TextStyle(color = c.inkMuted))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BasicText(if (heavy) "Native surface (today)" else "Readback", style = label)
        Segmented(SpikeMode.entries, mode, onMode, { it.label }, enabled = { !heavy })
        Segmented(SpikeResolution.entries, resolution, onResolution, { it.label }, enabled = { !heavy || it == SpikeResolution.Fit })
        ToggleRow("Spin", spinning, onSpinning, label)
        ToggleRow("Sway layout", sway, onSway, label)
        BasicText(header, style = small)
        readings.forEach { BasicText(it, style = small) }
    }
}

@Composable
private fun ToggleRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit, style: TextStyle) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Toggle(checked, onChange, text)
        BasicText(text, style = style)
    }
}
