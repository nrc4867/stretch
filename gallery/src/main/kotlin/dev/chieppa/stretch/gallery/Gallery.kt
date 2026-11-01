package dev.chieppa.stretch.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.ButtonKind
import dev.chieppa.stretch.ui.Chip
import dev.chieppa.stretch.ui.ChipKind
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.OptionChip
import dev.chieppa.stretch.ui.Segmented
import dev.chieppa.stretch.ui.Slider
import dev.chieppa.stretch.ui.Toggle
import dev.chieppa.stretch.ui.ToolButton
import kotlin.math.roundToInt

/** Every component in every theme. Labels here are examples, not app text. */
@Composable
fun Gallery() {
    var theme by remember { mutableStateOf(StretchThemes.Ink) }
    StretchTheme(theme) {
        val c = Stretch.colors
        Column(Modifier.fillMaxSize().background(c.surfaceCanvas)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(StretchSizes.header)
                    .background(c.surfaceRail)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Stretch", Stretch.type.heading)
                Text("Gallery", Stretch.type.body, muted = true)
                Spacer(Modifier.weight(1f))
                Segmented(StretchThemes.all, theme, { theme = it }, { it.name }, mode = true)
            }
            Spacer(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Section("Buttons") { ButtonsDemo() }
                Section("Toggles and sliders") { SettingsDemo() }
                Section("Chips") { ChipsDemo() }
                Section("Segmented") { SegmentedDemo() }
            }
        }
    }
}

@Composable
private fun Text(text: String, style: TextStyle, muted: Boolean = false) {
    val c = Stretch.colors
    BasicText(text, style = style.merge(TextStyle(color = if (muted) c.inkSubtle else c.inkStrong)))
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val c = Stretch.colors
    val shape = Stretch.shapes.cardShape
    Column(
        Modifier
            .widthIn(max = 720.dp)
            .clip(shape)
            .background(c.surfacePanel)
            .border(Stretch.stroke, c.linePanel, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(title.uppercase(), style = Stretch.type.eyebrow.merge(TextStyle(color = c.inkMuted)))
        content()
    }
}

@Composable
private fun ButtonsDemo() {
    var tool by remember { mutableStateOf("draw") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button("Duplicate", {})
        Button("Export", {}, kind = ButtonKind.Primary)
        Button("Reset", {}, kind = ButtonKind.Quiet)
        Button("Add layer", {}, icon = StretchIcons.Plus)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(StretchIcons.Undo, "Undo", {}, quiet = false)
        IconButton(StretchIcons.Redo, "Redo", {}, quiet = false)
        IconButton(StretchIcons.More, "More", {})
        IconButton(StretchIcons.Close, "Close", {}, size = StretchSizes.seg)
        Spacer(Modifier.width(12.dp))
        ToolButton(StretchIcons.Pen, "Draw", tool == "draw", { tool = "draw" })
        ToolButton(StretchIcons.Eraser, "Erase", tool == "erase", { tool = "erase" })
        ToolButton(StretchIcons.Move, "Move", tool == "move", { tool = "move" })
        ToolButton(StretchIcons.Mirror, "Mirror", tool == "mirror", { tool = "mirror" }, label = "Mirror")
    }
}

@Composable
private fun SettingsDemo() {
    var outline by remember { mutableStateOf(true) }
    var snap by remember { mutableStateOf(false) }
    var size by remember { mutableFloatStateOf(2.5f) }
    var opacity by remember { mutableFloatStateOf(0.85f) }
    SettingRow("Outline") { Spacer(Modifier.weight(1f)); Toggle(outline, { outline = it }, "Outline") }
    SettingRow("Snap") { Spacer(Modifier.weight(1f)); Toggle(snap, { snap = it }, "Snap") }
    SettingRow("Size") {
        Slider(size, { size = it }, "Size", Modifier.weight(1f), valueRange = 0.5f..8f)
        Readout("${(size * 10).roundToInt() / 10f} px")
    }
    SettingRow("Opacity") {
        Slider(opacity, { opacity = it }, "Opacity", Modifier.weight(1f))
        Readout("${(opacity * 100).roundToInt()}%")
    }
}

@Composable
private fun SettingRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.width(320.dp).height(StretchSizes.ctl),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(label, Modifier.width(76.dp), style = Stretch.type.body.merge(TextStyle(color = Stretch.colors.inkControl)))
        content()
    }
}

@Composable
private fun Readout(text: String) {
    BasicText(text, Modifier.width(52.dp), style = Stretch.type.value.merge(TextStyle(color = Stretch.colors.inkMuted)))
}

@Composable
private fun ChipsDemo() {
    val picks = remember { mutableStateOf(setOf("Pads", "Outline")) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Chip("Shape")
        Chip("4 layers", mono = true)
        Chip("Armed", kind = ChipKind.On)
        Chip("Gap · 0.4 px", kind = ChipKind.Warn)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (name in listOf("Pads", "Outline", "Fur edge", "Claws")) {
            OptionChip(name, name in picks.value, { on -> picks.value = if (on) picks.value + name else picks.value - name })
        }
    }
}

@Composable
private fun SegmentedDemo() {
    var view by remember { mutableStateOf("Side") }
    var mode by remember { mutableStateOf("Build") }
    var shading by remember { mutableStateOf("Two-tone") }
    Segmented(listOf("Side", "Front", "Top", "Under"), view, { view = it }, { it })
    Segmented(listOf("Build", "Pose", "Animate", "Scene"), mode, { mode = it }, { it }, mode = true)
    Segmented(listOf("Flat", "Two-tone"), shading, { shading = it }, { it }, Modifier.width(280.dp), fill = true)
}
