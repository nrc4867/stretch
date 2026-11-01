package dev.chieppa.stretch.gallery

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.core.Action
import dev.chieppa.stretch.core.ActionId
import dev.chieppa.stretch.core.ActionRegistry
import dev.chieppa.stretch.core.Applied
import dev.chieppa.stretch.core.Command
import dev.chieppa.stretch.core.History
import dev.chieppa.stretch.core.KeyChord
import dev.chieppa.stretch.core.Keymap
import dev.chieppa.stretch.frame.CommandPalette
import dev.chieppa.stretch.frame.DrawerPlacement
import dev.chieppa.stretch.frame.DrawerSection
import dev.chieppa.stretch.frame.DrawerSpec
import dev.chieppa.stretch.frame.FrameLayout
import dev.chieppa.stretch.frame.Hud
import dev.chieppa.stretch.frame.ListRow
import dev.chieppa.stretch.frame.ModeLayouts
import dev.chieppa.stretch.frame.RowIcon
import dev.chieppa.stretch.frame.RowText
import dev.chieppa.stretch.frame.SettingRow
import dev.chieppa.stretch.frame.Side
import dev.chieppa.stretch.frame.StretchApp
import dev.chieppa.stretch.frame.ToastHost
import dev.chieppa.stretch.frame.ToastState
import dev.chieppa.stretch.frame.Toolbar
import dev.chieppa.stretch.frame.TopBar
import dev.chieppa.stretch.frame.TopBarActions
import dev.chieppa.stretch.frame.Viewport
import dev.chieppa.stretch.frame.actionKeys
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.Segmented
import dev.chieppa.stretch.ui.Slider
import dev.chieppa.stretch.ui.Swatch
import dev.chieppa.stretch.ui.Toggle
import dev.chieppa.stretch.ui.ToolButton
import dev.chieppa.stretch.ui.formatNumber

/** A tiny document so the frame demo has real commands, undo and a palette. */
private data class DemoDoc(val layers: List<String>, val width: Float, val taper: Boolean)

private data class SetWidth(val width: Float) : Command<DemoDoc> {
    override fun apply(document: DemoDoc) = Applied(document.copy(width = width), SetWidth(document.width))
}

private data class SetTaper(val taper: Boolean) : Command<DemoDoc> {
    override fun apply(document: DemoDoc) = Applied(document.copy(taper = taper), SetTaper(document.taper))
}

private data class RemoveLayer(val name: String) : Command<DemoDoc> {
    override fun apply(document: DemoDoc): Applied<DemoDoc> {
        val index = document.layers.indexOf(name)
        return Applied(document.copy(layers = document.layers - name), InsertLayer(name, index))
    }
}

private data class InsertLayer(val name: String, val index: Int) : Command<DemoDoc> {
    override fun apply(document: DemoDoc) =
        Applied(document.copy(layers = document.layers.toMutableList().apply { add(index.coerceIn(0, size), name) }), RemoveLayer(name))
}

private enum class Mode { Build, Pose }

@Composable
fun FrameDemo(switcher: @Composable () -> Unit) {
    val history = remember { History(DemoDoc(listOf("Body", "Head", "Tail", "Fur edge"), 2.5f, true)) }
    val doc by history.document.collectAsState()
    val toasts = remember { ToastState() }
    var mode by remember { mutableStateOf(Mode.Build) }
    var selected by remember { mutableStateOf("Body") }
    var tool by remember { mutableStateOf("draw") }
    var palette by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf("Ochre") }
    val defaults = FrameLayout(
        listOf(
            DrawerPlacement("layers", Side.Left, open = true),
            DrawerPlacement("library", Side.Left),
            DrawerPlacement("brush", Side.Right, open = true),
            DrawerPlacement("colors", Side.Right, open = true),
        ),
    )
    var layouts by remember { mutableStateOf(ModeLayouts(Mode.entries.associate { it.name to defaults })) }

    val registry = remember {
        ActionRegistry().apply {
            register(Action(ActionId("undo"), "Undo", "undo", listOf(KeyChord.parse("Ctrl Z"))) { history.undo() })
            register(Action(ActionId("redo"), "Redo", "redo", listOf(KeyChord.parse("Ctrl Shift Z"))) { history.redo() })
            register(Action(ActionId("commands"), "Commands", "command", listOf(KeyChord.parse("Ctrl K"))) { palette = true })
            register(Action(ActionId("draw"), "Draw", "pen", listOf(KeyChord.parse("B"))) { tool = "draw" })
            register(Action(ActionId("erase"), "Erase", "eraser", listOf(KeyChord.parse("E"))) { tool = "erase" })
            register(Action(ActionId("taper"), "Toggle taper", null, listOf(KeyChord.parse("T"))) {
                history.execute(SetTaper(!history.document.value.taper))
            })
        }
    }
    val keymap = remember { Keymap(registry) }
    val icons: (String) -> ImageVector? = {
        when (it) {
            "undo" -> StretchIcons.Undo
            "redo" -> StretchIcons.Redo
            "command" -> StretchIcons.Command
            "pen" -> StretchIcons.Pen
            "eraser" -> StretchIcons.Eraser
            else -> null
        }
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val drawers = listOf(
        DrawerSpec("layers", "Layers", StretchIcons.Layers, count = doc.layers.size.toString()) {
            Column6 {
                for (layer in doc.layers) {
                    ListRow(layer == selected, { selected = layer }) {
                        RowIcon(StretchIcons.Eye)
                        RowText(layer, Modifier.weight(1f))
                    }
                }
            }
            DrawerSection(divider = false) {
                Button("Delete layer", {
                    if (selected in doc.layers) {
                        val name = selected
                        history.execute(RemoveLayer(name))
                        toasts.show("Deleted · $name", "Undo") { history.undo() }
                    }
                })
            }
        },
        DrawerSpec("library", "Library", StretchIcons.Grid) {
            DrawerSection(divider = false) { RowText("Shapes and presets live here.", subtle = true) }
        },
        DrawerSpec("brush", "Brush", StretchIcons.Pen) {
            DrawerSection("Outline", divider = false) {
                SettingRow("Width") {
                    var gesture by remember { mutableStateOf<History<DemoDoc>.Transaction?>(null) }
                    Slider(
                        doc.width,
                        { value ->
                            val g = gesture ?: history.begin().also { gesture = it }
                            g.update(SetWidth(value))
                        },
                        "Width",
                        Modifier.weight(1f),
                        valueRange = 0.5f..8f,
                        onValueChangeFinished = {
                            gesture?.commit()
                            gesture = null
                        },
                    )
                    BasicText(formatNumber(doc.width) + " px", Modifier.width(52.dp), style = Stretch.type.value.merge(TextStyle(color = Stretch.colors.inkMuted)))
                }
                SettingRow("Taper") { Spacer(Modifier.weight(1f)); Toggle(doc.taper, { history.execute(SetTaper(it)) }, "Taper") }
            }
        },
        DrawerSpec("colors", "Colors", StretchIcons.Theme) {
            DrawerSection(divider = false) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for ((name, c) in listOf("Ink" to Color(0xFF1C1517), "Ochre" to Color(0xFFE9A24B), "Cream" to Color(0xFFF4EAD8))) {
                        Swatch(c, name, color == name, { color = name })
                    }
                }
            }
        },
    )

    StretchApp(
        layout = layouts[mode.name],
        onLayoutChange = { layouts = layouts.update(mode.name) { _ -> it } },
        drawers = drawers,
        onResetLayout = { layouts = layouts.reset(mode.name) },
        modifier = Modifier.actionKeys(keymap, mode.name).focusRequester(focus).focusable(),
        topBar = {
            TopBar("Stretch", "Frame demo · draft", modes = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Segmented(Mode.entries, mode, { mode = it }, { it.name }, mode = true)
                    switcher()
                }
            }) {
                TopBarActions(onUndo = { history.undo() }, onRedo = { history.redo() }, onCommands = { palette = true })
            }
        },
    ) {
        Viewport {
            Toolbar(Modifier.align(Alignment.TopStart).padding(12.dp)) {
                ToolButton(StretchIcons.Pen, "Draw", tool == "draw", { tool = "draw" }, size = 34.dp)
                ToolButton(StretchIcons.Eraser, "Erase", tool == "erase", { tool = "erase" }, size = 34.dp)
                ToolButton(StretchIcons.Mirror, "Mirror", tool == "mirror", { tool = "mirror" }, size = 34.dp)
            }
            Hud("${selected} · ${formatNumber(doc.width)} px", Modifier.align(Alignment.BottomStart).padding(12.dp))
            Box(Modifier.align(Alignment.BottomCenter).padding(12.dp)) { ToastHost(toasts) }
        }
    }
    CommandPalette(palette, registry.available(mode.name).filter { it.id.value != "commands" }, keymap, { palette = false }, icons)
}

@Composable
private fun Column6(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
}
