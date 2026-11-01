package dev.chieppa.stretch.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.Dropdown
import dev.chieppa.stretch.frame.Hud
import dev.chieppa.stretch.frame.Menu
import dev.chieppa.stretch.frame.MenuDivider
import dev.chieppa.stretch.frame.MenuItem
import dev.chieppa.stretch.frame.MenuLabel
import dev.chieppa.stretch.frame.Popover
import dev.chieppa.stretch.frame.StretchDialog
import dev.chieppa.stretch.frame.ToastHost
import dev.chieppa.stretch.frame.ToastState
import dev.chieppa.stretch.frame.Toolbar
import dev.chieppa.stretch.frame.ToolbarSeparator
import dev.chieppa.stretch.frame.TopBar
import dev.chieppa.stretch.frame.SettingRow
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.ButtonKind
import dev.chieppa.stretch.ui.Callout
import dev.chieppa.stretch.ui.Chip
import dev.chieppa.stretch.ui.FieldIcon
import dev.chieppa.stretch.ui.Grid
import dev.chieppa.stretch.ui.Skeleton
import dev.chieppa.stretch.ui.SkeletonText
import dev.chieppa.stretch.ui.Spinner
import dev.chieppa.stretch.ui.ChipKind
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.Kbd
import dev.chieppa.stretch.ui.NumberField
import dev.chieppa.stretch.ui.OptionChip
import dev.chieppa.stretch.ui.ProgressBar
import dev.chieppa.stretch.ui.Segmented
import dev.chieppa.stretch.ui.Slider
import dev.chieppa.stretch.ui.Swatch
import dev.chieppa.stretch.ui.TextField
import dev.chieppa.stretch.ui.Toggle
import dev.chieppa.stretch.ui.Tooltip
import dev.chieppa.stretch.ui.ToolButton
import kotlin.math.roundToInt

private enum class Page(val label: String) { Components("Components"), Data("Data"), Tools("Tools"), Frame("App frame") }

/** The window's top bar: the plain [TopBar] in tests, the title bar when the gallery owns its window. */
typealias GalleryBar = @Composable (document: String, modes: @Composable () -> Unit, actions: @Composable RowScope.() -> Unit) -> Unit

private val plainBar: GalleryBar = { document, modes, actions -> TopBar("Stretch", document, modes = modes, actions = actions) }

/** Every component and the app frame in every theme. Labels here are examples, not app text. */
@Composable
fun Gallery(bar: GalleryBar = plainBar) {
    var theme by remember { mutableStateOf(StretchThemes.Ink) }
    var page by remember { mutableStateOf(Page.Components) }
    StretchTheme(theme) {
        val switcher: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Segmented(Page.entries, page, { page = it }, { it.label }, mode = true)
                Dropdown(StretchThemes.all, theme, { theme = it }, { it.name }, "Theme", Modifier.width(180.dp))
            }
        }
        when (page) {
            Page.Components -> Column(Modifier.fillMaxSize().background(Stretch.colors.surfaceCanvas)) {
                bar("Gallery", switcher) {}
                ComponentsPage()
            }
            Page.Data -> Column(Modifier.fillMaxSize().background(Stretch.colors.surfaceCanvas)) {
                bar("Gallery", switcher) {}
                DataPage()
            }
            Page.Tools -> Column(Modifier.fillMaxSize().background(Stretch.colors.surfaceCanvas)) {
                bar("Gallery", switcher) {}
                ToolsPage()
            }
            Page.Frame -> FrameDemo(switcher, bar)
        }
    }
}

@Composable
private fun ComponentsPage() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Section("Buttons") { ButtonsDemo() }
        Section("Toggles, sliders and fields") { SettingsDemo() }
        Section("Chips and swatches") { ChipsDemo() }
        Section("Segmented") { SegmentedDemo() }
        Section("Menus, tooltips and dialogs") { OverlaysDemo() }
        Section("Feedback") { FeedbackDemo() }
        Section("Loading") { LoadingDemo() }
        Section("Grid") { GridDemo() }
        Section("Toolbar and readouts") { ToolbarDemo() }
        Section("Icons") { IconsDemo() }
    }
}

@Composable
internal fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val c = Stretch.colors
    val shape = Stretch.shapes.cardShape
    Column(
        Modifier
            .widthIn(max = 760.dp)
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
    var spread by remember { mutableFloatStateOf(18f) }
    var blend by remember { mutableStateOf("Normal") }
    var search by remember { mutableStateOf("") }
    Column(Modifier.width(340.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
        SettingRow("Spread") { Spacer(Modifier.weight(1f)); NumberField(spread, { spread = it }, "Spread", unit = "°", range = 0f..90f) }
        SettingRow("Blend") { Dropdown(listOf("Normal", "Multiply", "Screen"), blend, { blend = it }, { it }, "Blend", Modifier.weight(1f)) }
        TextField(search, { search = it }, placeholder = "Search commands", icon = StretchIcons.Search)
        var name by remember { mutableStateOf("Fox") }
        TextField(
            name, { name = it }, placeholder = "Character name",
            trailing = { Tooltip("Shown on exports and in the file name") { FieldIcon(StretchIcons.Help, contentDescription = "About names") } },
        )
        SettingRow("Line") {
            Spacer(Modifier.weight(1f))
            NumberField(spread, { spread = it }, "Line weight", unit = "px", width = 104.dp, leading = { FieldIcon(StretchIcons.Pen) })
        }
    }
}

@Composable
private fun Readout(text: String) {
    BasicText(text, Modifier.width(52.dp), style = Stretch.type.value.merge(TextStyle(color = Stretch.colors.inkMuted)))
}

@Composable
private fun ChipsDemo() {
    var picks by remember { mutableStateOf(setOf("Pads", "Outline")) }
    var swatch by remember { mutableStateOf("Ochre") }
    val swatches = listOf(
        "Ink" to Color(0xFF1C1517), "Ochre" to Color(0xFFE9A24B), "Ochre shade" to Color(0xFFC27F35),
        "Cream" to Color(0xFFF4EAD8), "Slate blue" to Color(0xFF6C8FB3), "Sage" to Color(0xFF8FB07A),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Chip("Shape")
        Chip("4 layers", mono = true)
        Chip("Armed", kind = ChipKind.On)
        Chip("Gap · 0.4 px", kind = ChipKind.Warn)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (name in listOf("Pads", "Outline", "Fur edge", "Claws")) {
            OptionChip(name, name in picks, { on -> picks = if (on) picks + name else picks - name })
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        for ((name, color) in swatches) Tooltip(name) { Swatch(color, name, swatch == name, { swatch = name }) }
        Spacer(Modifier.width(8.dp))
        Readout(swatch)
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

@Composable
private fun OverlaysDemo() {
    var menu by remember { mutableStateOf(false) }
    var dock by remember { mutableStateOf("Left") }
    var dialog by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(2f) }
    var popover by remember { mutableStateOf(false) }
    var width by remember { mutableFloatStateOf(2.5f) }
    var taper by remember { mutableStateOf(true) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            Button("Open menu", { menu = true }, icon = StretchIcons.More)
            Menu(menu, { menu = false }) {
                MenuItem("Duplicate", { menu = false }, icon = StretchIcons.Plus, keys = "Ctrl D")
                MenuItem("Mirror to other side", { menu = false }, icon = StretchIcons.Mirror, keys = "M")
                MenuItem("Lock", { menu = false }, icon = StretchIcons.Lock, keys = "Ctrl L")
                MenuDivider()
                MenuLabel("Dock")
                for (side in listOf("Left", "Right", "Float")) MenuItem(side, { dock = side; menu = false }, checked = dock == side)
            }
        }
        Tooltip("Mirror to other side", listOf("M")) { IconButton(StretchIcons.Mirror, "Mirror", {}, quiet = false) }
        Box {
            Button("Brush settings", { popover = true }, icon = StretchIcons.Pen)
            Popover(popover, { popover = false }, title = "Brush") {
                SettingRow("Width") { Slider(width, { width = it }, "Width", Modifier.weight(1f), valueRange = 0.5f..8f) }
                SettingRow("Taper") { Spacer(Modifier.weight(1f)); Toggle(taper, { taper = it }, "Taper") }
            }
        }
        Box {
            Button("Export", { dialog = true }, icon = StretchIcons.Export)
            if (dialog) {
                StretchDialog(
                    "Export",
                    onDismiss = { dialog = false },
                    icon = StretchIcons.Export,
                    actions = {
                        Button("Cancel", { dialog = false }, kind = ButtonKind.Quiet)
                        Button("Export", { dialog = false }, kind = ButtonKind.Primary)
                    },
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        var format by remember { mutableStateOf("PNG") }
                        var outline by remember { mutableStateOf(true) }
                        SettingRow("Format") { Segmented(listOf("PNG", "glTF"), format, { format = it }, { it }, Modifier.weight(1f), fill = true) }
                        SettingRow("Scale") { Spacer(Modifier.weight(1f)); NumberField(scale, { scale = it }, "Scale", unit = "×", range = 1f..8f) }
                        SettingRow("Outline") { Spacer(Modifier.weight(1f)); Toggle(outline, { outline = it }, "Outline") }
                    }
                }
            }
        }
        Kbd(listOf("Ctrl", "K"))
    }
}

@Composable
private fun FeedbackDemo() {
    val toasts = remember { ToastState() }
    Callout("Gap in outline", "Close gap", {}, Modifier.width(380.dp), value = "0.4 px")
    Row(Modifier.width(380.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        ProgressBar(0.6f, "Baking", Modifier.weight(1f))
        IconButton(StretchIcons.Close, "Cancel", {}, size = StretchSizes.seg)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Button("Delete", { toasts.show("Deleted · Fur edge", "Undo") })
        ToastHost(toasts)
    }
}

@Composable
private fun LoadingDemo() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Spinner("Saving")
        Spinner("Loading library", size = 24.dp)
        Column(Modifier.width(200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Skeleton(Modifier.size(40.dp), Stretch.shapes.cardShape)
                SkeletonText(2, Modifier.weight(1f))
            }
            Skeleton(Modifier.fillMaxWidth().height(32.dp))
        }
    }
}

@Composable
private fun GridDemo() {
    Grid(minCell = 120.dp, gap = 8.dp) {
        for (n in 1..7) {
            Box(Modifier.height(72.dp).clip(Stretch.shapes.cardShape).background(Stretch.colors.surfaceControl), contentAlignment = Alignment.Center) {
                BasicText("Pose $n", style = Stretch.type.bodySm.merge(TextStyle(color = Stretch.colors.ink)))
            }
        }
    }
}

@Composable
private fun ToolbarDemo() {
    var tool by remember { mutableStateOf("draw") }
    var view by remember { mutableStateOf("Side") }
    Toolbar {
        ToolButton(StretchIcons.Pen, "Draw", tool == "draw", { tool = "draw" }, size = 34.dp)
        ToolButton(StretchIcons.Push, "Push", tool == "push", { tool = "push" }, size = 34.dp)
        ToolButton(StretchIcons.Smooth, "Smooth", tool == "smooth", { tool = "smooth" }, size = 34.dp)
        ToolButton(StretchIcons.Eraser, "Erase", tool == "erase", { tool = "erase" }, size = 34.dp)
        ToolbarSeparator()
        Segmented(listOf("Side", "Front", "Top", "Under"), view, { view = it }, { it })
    }
    Hud("18° · 2.5 px")
}

@Composable
private fun IconsDemo() {
    val icons = listOf(
        "plus" to StretchIcons.Plus, "minus" to StretchIcons.Minus, "close" to StretchIcons.Close, "more" to StretchIcons.More,
        "check" to StretchIcons.Check, "search" to StretchIcons.Search, "undo" to StretchIcons.Undo, "redo" to StretchIcons.Redo,
        "eye" to StretchIcons.Eye, "eye-off" to StretchIcons.EyeOff, "lock" to StretchIcons.Lock, "settings" to StretchIcons.Settings,
        "layers" to StretchIcons.Layers, "pen" to StretchIcons.Pen, "eraser" to StretchIcons.Eraser, "push" to StretchIcons.Push,
        "smooth" to StretchIcons.Smooth, "mirror" to StretchIcons.Mirror, "move" to StretchIcons.Move, "rotate" to StretchIcons.Rotate,
        "grid" to StretchIcons.Grid, "camera" to StretchIcons.Camera, "play" to StretchIcons.Play, "pause" to StretchIcons.Pause,
        "keyframe" to StretchIcons.Keyframe, "folder" to StretchIcons.Folder, "save" to StretchIcons.Save, "export" to StretchIcons.Export,
        "warn" to StretchIcons.Warn, "error" to StretchIcons.Error, "info" to StretchIcons.Info, "help" to StretchIcons.Help, "drag" to StretchIcons.Drag,
        "pin" to StretchIcons.Pin, "dock-left" to StretchIcons.DockLeft, "dock-right" to StretchIcons.DockRight,
        "command" to StretchIcons.Command, "theme" to StretchIcons.Theme, "chevron-down" to StretchIcons.ChevronDown,
        "chevron-right" to StretchIcons.ChevronRight, "dock-bottom" to StretchIcons.DockBottom, "import" to StretchIcons.Import,
        "file" to StretchIcons.File, "arrow-up" to StretchIcons.ArrowUp, "arrow-down" to StretchIcons.ArrowDown,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for ((name, icon) in icons) {
            Row(
                Modifier.width(112.dp).clip(Stretch.shapes.segShape).background(Stretch.colors.surfaceControl).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, Stretch.colors.inkControl)
                BasicText(name, style = Stretch.type.valueSm.merge(TextStyle(color = Stretch.colors.inkMuted)))
            }
        }
    }
}
