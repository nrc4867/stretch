package dev.chieppa.stretch.gallery

import dev.chieppa.stretch.ui.opensOnPress
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.ContextMenuArea
import dev.chieppa.stretch.frame.DrawerSection
import dev.chieppa.stretch.frame.MenuDivider
import dev.chieppa.stretch.frame.MenuItem
import dev.chieppa.stretch.frame.Popover
import dev.chieppa.stretch.frame.SettingRow
import dev.chieppa.stretch.frame.StatusBar
import dev.chieppa.stretch.frame.StatusItem
import dev.chieppa.stretch.frame.ViewNavigator
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.AngleDial
import dev.chieppa.stretch.ui.BadgedBox
import dev.chieppa.stretch.ui.Checkbox
import dev.chieppa.stretch.ui.ColorPicker
import dev.chieppa.stretch.ui.CurveEditor
import dev.chieppa.stretch.ui.EmptyState
import dev.chieppa.stretch.ui.NumberField
import dev.chieppa.stretch.ui.PlaybackEffect
import dev.chieppa.stretch.ui.RadioGroup
import dev.chieppa.stretch.ui.RampEditor
import dev.chieppa.stretch.ui.RampStop
import dev.chieppa.stretch.ui.Swatch
import dev.chieppa.stretch.ui.Tabs
import dev.chieppa.stretch.ui.TextField
import dev.chieppa.stretch.ui.Timeline
import dev.chieppa.stretch.ui.TimelineKey
import dev.chieppa.stretch.ui.TimelineTrack
import dev.chieppa.stretch.ui.ToolButton
import dev.chieppa.stretch.ui.Tree
import dev.chieppa.stretch.ui.TreeDrop
import dev.chieppa.stretch.ui.TreeNode
import dev.chieppa.stretch.ui.badgeCount
import dev.chieppa.stretch.ui.parseHex
import dev.chieppa.stretch.ui.toHex

@Composable
internal fun ToolsPage() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Section("Tree") { TreeDemo() }
        Section("Choices") { ChoicesDemo() }
        Section("Tabs, badges and empty states") { TabsDemo() }
        Section("Context menu and sections") { ContextDemo() }
        Section("Color") { ColorDemo() }
        Section("Shading ramp") { RampDemo() }
        Section("Curve and angle") { CurveDemo() }
        Section("Timeline") { TimelineDemo() }
        Section("Navigator and status bar") { NavigatorDemo() }
    }
}

/** A small editable tree of parts: nodes are names, children are looked up by name. */
private fun buildTree(children: Map<String?, List<String>>, parent: String? = null): List<TreeNode<String>> =
    children[parent].orEmpty().map { TreeNode(it, buildTree(children, it)) }

private fun Map<String?, List<String>>.moved(key: String, drop: TreeDrop): Map<String?, List<String>> {
    val without = mapValues { (_, list) -> list - key }
    val target = drop.parent as String?
    val list = without[target].orEmpty().toMutableList().apply { add(drop.index.coerceIn(0, size), key) }
    return without + (target to list)
}

@Composable
private fun TreeDemo() {
    var children by remember {
        mutableStateOf<Map<String?, List<String>>>(
            mapOf(null to listOf("Body", "Props"), "Body" to listOf("Head", "Front paws", "Tail"), "Head" to listOf("Ears", "Snout"), "Props" to listOf("Scarf")),
        )
    }
    var open by remember { mutableStateOf(setOf<Any>("Body")) }
    var selection by remember { mutableStateOf(setOf<Any>("Head")) }
    Tree(
        buildTree(children), { it }, { it }, open, { k, o -> open = if (o) open + k else open - k },
        selection, { selection = it }, Modifier.width(320.dp),
        icon = { if (children[it].isNullOrEmpty()) StretchIcons.Eye else StretchIcons.Layers },
        onRename = { old, new ->
            children = children.mapValues { (_, l) -> l.map { if (it == old) new else it } }.mapKeys { (k, _) -> if (k == old) new else k }
        },
        onMove = { k, drop -> children = children.moved(k as String, drop) },
        leading = { if (it == "Scarf") Box(Modifier.size(12.dp).background(Stretch.colors.chart2, Stretch.shapes.segShape)) },
    )
}

@Composable
private fun ChoicesDemo() {
    var parts by remember { mutableStateOf(setOf("Body", "Head")) }
    val all = listOf("Body", "Head", "Tail")
    var scope by remember { mutableStateOf("This part") }
    var name by remember { mutableStateOf("fox?") }
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Column {
            Checkbox(if (parts.size == all.size) true else if (parts.isEmpty()) false else null, { parts = if (it) all.toSet() else emptySet() }, "All parts")
            for (p in all) Checkbox(p in parts, { on -> parts = if (on) parts + p else parts - p }, p, Modifier.padding(start = 20.dp))
        }
        RadioGroup(listOf("This part", "Both sides", "All parts"), scope, { scope = it }, { it })
        TextField(name, { name = it }, Modifier.width(220.dp), placeholder = "File name", error = if ('?' in name) "File names can't contain ?" else null)
    }
}

@Composable
private fun TabsDemo() {
    var docs by remember { mutableStateOf(listOf("Fox", "Owl", "Badger")) }
    var current by remember { mutableStateOf("Fox") }
    Tabs(docs, current, { current = it }, { it }, onClose = { d -> docs = docs - d; if (current == d) current = docs.firstOrNull() ?: "" }, closeDescription = { "Close $it" })
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        BadgedBox(badgeCount(3), "Library · 3") { ToolButton(StretchIcons.Grid, "Library", false, {}) }
        BadgedBox("", "Layers · new") { ToolButton(StretchIcons.Layers, "Layers", true, {}) }
        BadgedBox(badgeCount(140), "Exports · 99+") { ToolButton(StretchIcons.Export, "Exports", false, {}) }
        Box(Modifier.width(260.dp)) { EmptyState(StretchIcons.Layers, "No layers", actionLabel = "Add layer") }
    }
}

@Composable
private fun ContextDemo() {
    var last by remember { mutableStateOf("Right-click or long-press the board") }
    var outline by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ContextMenuArea({ close ->
            MenuItem("Duplicate", { last = "Duplicate"; close() }, icon = StretchIcons.Plus, keys = "Ctrl D")
            MenuItem("Mirror to other side", { last = "Mirror"; close() }, icon = StretchIcons.Mirror, keys = "M")
            MenuDivider()
            MenuItem("Lock", { last = "Lock"; close() }, icon = StretchIcons.Lock)
        }) {
            Box(
                Modifier.size(280.dp, 140.dp).clip(Stretch.shapes.cardShape).background(Stretch.colors.surfaceWell),
                contentAlignment = Alignment.Center,
            ) { BasicText(last, style = Stretch.type.bodySm.merge(TextStyle(color = Stretch.colors.ink))) }
        }
        Column(Modifier.width(300.dp).clip(Stretch.shapes.cardShape).background(Stretch.colors.surfacePanel)) {
            DrawerSection("Outline", collapsed = outline, onCollapsedChange = { outline = it }) {
                SettingRow("Width") { NumberField(2.5f, {}, "Width", unit = "px") }
            }
        }
    }
}

@Composable
private fun ColorDemo() {
    var color by remember { mutableStateOf(parseHex("#E9A24B")!!) }
    val recent = listOf("#1C1517", "#E9A24B", "#C27F35", "#F4EAD8", "#3366CC").map { parseHex(it)!! }
    var popover by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
        ColorPicker(color, { color = it }, recent = recent, onPickFromScreen = {})
        Box {
            Swatch(color, color.toHex(), popover, { popover = true }, Modifier.opensOnPress(popover) { popover = true })
            Popover(popover, { popover = false }, title = "Fill", width = 268.dp) { ColorPicker(color, { color = it }, recent = recent) }
        }
    }
}

@Composable
private fun RampDemo() {
    var stops by remember {
        mutableStateOf(listOf(RampStop(0f, parseHex("#3B2A2E")!!), RampStop(0.35f, parseHex("#C27F35")!!), RampStop(0.7f, parseHex("#E9A24B")!!), RampStop(0.92f, parseHex("#FCE6B8")!!)))
    }
    var picked by remember { mutableIntStateOf(1) }
    var smooth by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RampEditor(stops, { stops = it }, picked, { picked = it }, smooth = smooth)
            Checkbox(smooth, { smooth = it }, "Blend bands")
        }
        ColorPicker(stops[picked.coerceIn(stops.indices)].color, { c -> stops = stops.mapIndexed { i, s -> if (i == picked) s.copy(color = c) else s } })
    }
}

@Composable
private fun CurveDemo() {
    var points by remember { mutableStateOf(listOf(Offset(0f, 0f), Offset(0.35f, 0.15f), Offset(1f, 1f))) }
    var angle by remember { mutableFloatStateOf(45f) }
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
        CurveEditor(points, { points = it }, description = "Pen pressure")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AngleDial(angle, { angle = it }, "Light direction")
            NumberField(angle, { angle = ((it % 360f) + 360f) % 360f }, "Light angle", unit = "°", range = 0f..360f)
        }
    }
}

@Composable
private fun TimelineDemo() {
    var frame by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var tracks by remember {
        mutableStateOf(listOf(TimelineTrack("head", "Head", listOf(0, 12, 24)), TimelineTrack("tail", "Tail", listOf(0, 6, 18, 30)), TimelineTrack("ears", "Ears", listOf(4, 20))))
    }
    var selected by remember { mutableStateOf(setOf<TimelineKey>()) }
    PlaybackEffect(playing, 24, 48, frame) { frame = it }
    Timeline(
        48, frame, { frame = it }, tracks, playing, { playing = it }, Modifier.fillMaxWidth().clip(Stretch.shapes.cardShape).background(Stretch.colors.surfacePanel),
        selected = selected, onSelectedChange = { selected = it },
        onMoveKeys = { keys, delta ->
            tracks = tracks.map { t -> t.copy(keys = t.keys.map { k -> if (TimelineKey(t.id, k) in keys) (k + delta).coerceIn(0, 47) else k }.distinct().sorted()) }
            selected = keys.map { it.copy(frame = (it.frame + delta).coerceIn(0, 47)) }.toSet()
        },
    )
}

@Composable
private fun NavigatorDemo() {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset(0.2f, 0.2f)) }
    val view = Rect(pan, androidx.compose.ui.geometry.Size(0.5f / zoom.coerceAtLeast(0.5f), 0.5f / zoom.coerceAtLeast(0.5f)))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().height(260.dp).clip(Stretch.shapes.cardShape).background(Stretch.colors.surfaceWell)) {
            ViewNavigator(zoom, { zoom = it }, { zoom = 1f; pan = Offset(0.25f, 0.25f) }, Modifier.align(Alignment.TopEnd).padding(12.dp), minimap = {
                Canvas(Modifier.matchParentSize()) {
                    drawOval(Color(0xFFE9A24B), Offset(size.width * 0.3f, size.height * 0.35f), androidx.compose.ui.geometry.Size(size.width * 0.4f, size.height * 0.35f))
                    drawCircle(Color(0xFF1C1517), size.minDimension * 0.08f, Offset(size.width * 0.66f, size.height * 0.36f))
                }
            }, visible = view, onPan = { pan = it })
        }
        StatusBar(start = {
            StatusItem("Baking shading · 60%", StretchIcons.Layers)
            StatusItem("3 parts selected")
        }, end = {
            StatusItem("px")
            StatusItem("${(zoom * 100).toInt()}%", onClick = { zoom = 1f })
        })
    }
}
