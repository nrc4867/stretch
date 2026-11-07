package dev.chieppa.stretch.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.chieppa.stretch.document.DocumentFormat
import dev.chieppa.stretch.document.DocumentSession
import dev.chieppa.stretch.document.RecentFiles
import dev.chieppa.stretch.document.RecentFilesMenuItems
import dev.chieppa.stretch.document.RecoveredDocument
import dev.chieppa.stretch.document.RecoveryDialog
import dev.chieppa.stretch.document.UnsavedChangesDialog
import dev.chieppa.stretch.document.Autosave
import dev.chieppa.stretch.document.LeaveGuard
import dev.chieppa.stretch.document.documentStorage
import dev.chieppa.stretch.document.documentName
import dev.chieppa.stretch.document.rememberEdited
import dev.chieppa.stretch.document.rememberFileChooser
import dev.chieppa.stretch.frame.ActionMenuItem
import dev.chieppa.stretch.frame.BarAction
import dev.chieppa.stretch.frame.BarMenu
import dev.chieppa.stretch.frame.BarMenus
import dev.chieppa.stretch.frame.capsFor
import dev.chieppa.stretch.frame.BoxTransform
import dev.chieppa.stretch.frame.ClipData
import dev.chieppa.stretch.frame.ClipboardHandler
import dev.chieppa.stretch.frame.Guide
import dev.chieppa.stretch.frame.Menu
import dev.chieppa.stretch.frame.MenuDivider
import dev.chieppa.stretch.frame.MenuItem
import dev.chieppa.stretch.frame.ViewportCamera
import dev.chieppa.stretch.frame.clipboardTarget
import dev.chieppa.stretch.frame.marquee
import dev.chieppa.stretch.frame.rememberClipboardRouter
import dev.chieppa.stretch.frame.transformHandles
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import dev.chieppa.stretch.frame.DrawerPosition
import dev.chieppa.stretch.frame.DrawerSection
import dev.chieppa.stretch.frame.DrawerSpec
import dev.chieppa.stretch.frame.HistoryPanel
import dev.chieppa.stretch.frame.KeymapEditor
import dev.chieppa.stretch.frame.FrameLayout
import dev.chieppa.stretch.frame.Hud
import dev.chieppa.stretch.frame.KeyCastState
import dev.chieppa.stretch.frame.ListRow
import dev.chieppa.stretch.frame.ModeLayouts
import dev.chieppa.stretch.frame.Popover
import dev.chieppa.stretch.frame.RowIcon
import dev.chieppa.stretch.frame.RowText
import dev.chieppa.stretch.frame.SettingRow
import dev.chieppa.stretch.frame.Side
import dev.chieppa.stretch.frame.StatusBar
import dev.chieppa.stretch.frame.StatusItem
import dev.chieppa.stretch.frame.StretchApp
import dev.chieppa.stretch.frame.ToastHost
import dev.chieppa.stretch.frame.ToastState
import dev.chieppa.stretch.frame.Toolbar
import dev.chieppa.stretch.frame.TopBar
import dev.chieppa.stretch.frame.TopBarActions
import dev.chieppa.stretch.frame.ViewNavigator
import dev.chieppa.stretch.frame.Viewport
import dev.chieppa.stretch.frame.actionKeys
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.Grid
import dev.chieppa.stretch.ui.PlaybackEffect
import dev.chieppa.stretch.ui.Segmented
import dev.chieppa.stretch.ui.Slider
import dev.chieppa.stretch.ui.Swatch
import dev.chieppa.stretch.ui.Timeline
import dev.chieppa.stretch.ui.TimelineTrack
import dev.chieppa.stretch.ui.Toggle
import dev.chieppa.stretch.ui.ToolButton
import dev.chieppa.stretch.ui.formatNumber

/** A tiny document so the frame demo has real commands, undo, a palette, files and a canvas. */
private data class DemoDoc(
    val layers: List<String>,
    val width: Float,
    val taper: Boolean,
    val shapes: Map<String, BoxTransform> = emptyMap(),
)

private val startingDoc = DemoDoc(
    layers = listOf("Body", "Head", "Tail", "Fur edge"),
    width = 2.5f,
    taper = true,
    shapes = mapOf(
        "Body" to BoxTransform(Rect(0f, 0f, 240f, 160f)),
        "Head" to BoxTransform(Rect(190f, -130f, 330f, 10f), 12f),
        "Tail" to BoxTransform(Rect(-150f, 50f, -10f, 100f), -20f),
        "Fur edge" to BoxTransform(Rect(20f, 150f, 220f, 190f)),
    ),
)

private val defaultShape = BoxTransform(Rect(0f, 0f, 120f, 80f))

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

private data class AddLayer(val name: String, val shape: BoxTransform) : Command<DemoDoc> {
    override fun apply(document: DemoDoc) =
        Applied(document.copy(layers = document.layers + name, shapes = document.shapes + (name to shape)), RemoveLayer(name))
}

private data class SetShape(val name: String, val shape: BoxTransform) : Command<DemoDoc> {
    override fun apply(document: DemoDoc) =
        Applied(document.copy(shapes = document.shapes + (name to shape)), SetShape(name, document.shapes[name] ?: defaultShape))
}

/** How the history panel names each kind of step. */
private fun stepLabel(command: Command<DemoDoc>): String = when (command) {
    is SetWidth -> "Width ${formatNumber(command.width)} px"
    is SetTaper -> if (command.taper) "Taper on" else "Taper off"
    is RemoveLayer -> "Delete ${command.name}"
    is InsertLayer -> "Restore ${command.name}"
    is AddLayer -> "Paste ${command.name}"
    is SetShape -> "Transform ${command.name}"
    else -> "Change"
}

private fun BoxTransform.encode() = listOf(bounds.left, bounds.top, bounds.right, bounds.bottom, rotation).joinToString(",")

private fun decodeShape(text: String): BoxTransform? {
    val n = text.split(',').mapNotNull { it.trim().toFloatOrNull() }
    return if (n.size == 5) BoxTransform(Rect(n[0], n[1], n[2], n[3]), n[4]) else null
}

/** The demo's file format: one setting or layer per line. */
private object DemoFormat : DocumentFormat<DemoDoc> {
    override val extension = "stretchdemo"
    override val description = "Stretch demo"

    override fun encode(document: DemoDoc) = buildString {
        appendLine("width=${document.width}")
        appendLine("taper=${document.taper}")
        for (layer in document.layers) appendLine("layer=$layer|${(document.shapes[layer] ?: defaultShape).encode()}")
    }.encodeToByteArray()

    override fun decode(bytes: ByteArray): DemoDoc {
        var doc = DemoDoc(emptyList(), 2.5f, true)
        for (line in bytes.decodeToString().lines()) {
            val (key, value) = line.split('=', limit = 2).takeIf { it.size == 2 } ?: continue
            doc = when (key) {
                "width" -> doc.copy(width = value.toFloatOrNull() ?: doc.width)
                "taper" -> doc.copy(taper = value.toBoolean())
                "layer" -> {
                    val name = value.substringBefore('|')
                    doc.copy(layers = doc.layers + name, shapes = doc.shapes + (name to (decodeShape(value.substringAfter('|')) ?: defaultShape)))
                }
                else -> doc
            }
        }
        return doc
    }
}

/** What the canvas copies: one layer's name and shape. */
private const val LayerClip = "stretch-gallery/layer"

private fun sceneBounds(doc: DemoDoc): Rect =
    doc.layers.mapNotNull { doc.shapes[it]?.bounds }.reduceOrNull { a, b -> Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom)) }
        ?: Rect(0f, 0f, 1f, 1f)

/** Whether scene point [p] is inside [shape], rotation included. */
private fun BoxTransform.contains(p: Offset): Boolean {
    val r = -rotation * PI.toFloat() / 180f
    val d = p - bounds.center
    return bounds.contains(bounds.center + Offset(d.x * cos(r) - d.y * sin(r), d.x * sin(r) + d.y * cos(r)))
}

private enum class Mode { Build, Pose }

private fun ActionRegistry.named(id: String): Action = checkNotNull(get(ActionId(id)))

@Composable
fun FrameDemo(switcher: @Composable () -> Unit, bar: GalleryBar, scale: Float, onScale: (Float) -> Unit) {
    val host = LocalGalleryHost.current
    val session = remember { DocumentSession(DemoFormat, startingDoc, documentStorage) }
    val history = session.history
    val doc by history.document.collectAsState()
    val edited = rememberEdited(session)
    val chooser = rememberFileChooser()
    val recent = remember { RecentFiles() }
    val scope = rememberCoroutineScope()
    val toasts = remember { ToastState() }
    val router = rememberClipboardRouter()
    val camera = remember { ViewportCamera() }
    var mode by remember { mutableStateOf(Mode.Build) }
    var selected by remember { mutableStateOf("Body") }
    var tool by remember { mutableStateOf("draw") }
    var palette by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf("Ochre") }
    var showKeys by remember { mutableStateOf(false) }
    var rulers by remember { mutableStateOf(true) }
    var layerStrip by remember { mutableStateOf(false) }
    var guides by remember { mutableStateOf(listOf(Guide(vertical = true, position = 0f))) }
    var settings by remember { mutableStateOf(false) }
    var fileMenu by remember { mutableStateOf(false) }
    val keyCast = remember { KeyCastState() }
    val recovery = host.recovery
    var recovered by remember { mutableStateOf(emptyList<RecoveredDocument>()) }
    LaunchedEffect(recovery) { recovered = recovery?.pending().orEmpty() }
    LeaveGuard(edited)
    if (recovery != null) Autosave(session, recovery, intervalMillis = 10_000)
    DisposableEffect(session) {
        host.closeGuard = { then -> session.whenSaved(then) }
        onDispose { host.closeGuard = null }
    }

    fun save(asNew: Boolean = false) {
        scope.launch {
            runCatching { if (asNew) session.saveAs(chooser) else session.save(chooser) }
                .onSuccess { saved -> if (saved) session.path?.let { recent.add(it); toasts.show("Saved ${session.fileName}") } }
                .onFailure { toasts.show("Couldn't save: ${it.message}") }
        }
    }

    fun open(path: String) {
        session.whenSaved {
            scope.launch {
                runCatching { session.open(path) }
                    .onSuccess { recent.add(path); selected = session.history.document.value.layers.firstOrNull().orEmpty() }
                    .onFailure { recent.remove(path); toasts.show("Couldn't open ${documentStorage.displayName(path)}") }
            }
        }
    }

    fun chooseAndOpen() = session.whenSaved { scope.launch { chooser.chooseOpen(DemoFormat)?.let(::open) } }

    val defaults = FrameLayout(
        listOf(
            DrawerPlacement("layers", Side.Left, open = true),
            DrawerPlacement("library", Side.Left, bottom = true),
            DrawerPlacement("brush", Side.Right, open = true),
            DrawerPlacement("colors", Side.Right, open = true),
            DrawerPlacement("help", Side.Left, bottom = true),
            DrawerPlacement("history", Side.Left),
            DrawerPlacement("timeline", Side.Right, bottom = true, open = true),
        ),
    )
    var layouts by remember { mutableStateOf(ModeLayouts(Mode.entries.associate { it.name to defaults })) }

    val registry = remember {
        ActionRegistry().apply {
            register(Action(ActionId("undo"), "Undo", "undo", listOf(KeyChord.parse("Ctrl Z"))) { session.history.undo() })
            register(Action(ActionId("redo"), "Redo", "redo", listOf(KeyChord.parse("Ctrl Shift Z"))) { session.history.redo() })
            register(Action(ActionId("commands"), "Commands", "command", listOf(KeyChord.parse("Ctrl K"))) { palette = true })
            register(Action(ActionId("new"), "New", "file", listOf(KeyChord.parse("Ctrl N"))) { session.whenSaved { session.new(startingDoc) } })
            register(Action(ActionId("open"), "Open", "folder", listOf(KeyChord.parse("Ctrl O"))) { chooseAndOpen() })
            register(Action(ActionId("save"), "Save", "save", listOf(KeyChord.parse("Ctrl S"))) { save() })
            register(Action(ActionId("save-as"), "Save as", null, listOf(KeyChord.parse("Ctrl Shift S"))) { save(asNew = true) })
            router.actions("Copy", "Cut", "Paste").forEach(::register)
            register(Action(ActionId("draw"), "Draw", "pen", listOf(KeyChord.parse("B"))) { tool = "draw" })
            register(Action(ActionId("erase"), "Erase", "eraser", listOf(KeyChord.parse("E"))) { tool = "erase" })
            register(Action(ActionId("taper"), "Toggle taper", null, listOf(KeyChord.parse("T"))) {
                session.history.execute(SetTaper(!session.history.document.value.taper))
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
            "file" -> StretchIcons.File
            "folder" -> StretchIcons.Folder
            "save" -> StretchIcons.Save
            else -> null
        }
    }

    val layerClipboard = object : ClipboardHandler {
        override val types = setOf(LayerClip)

        override fun copy(): ClipData? {
            val d = session.history.document.value
            if (selected !in d.layers) return null
            return ClipData(selected, mapOf(LayerClip to "$selected|${(d.shapes[selected] ?: defaultShape).encode()}"))
        }

        override fun cut(): ClipData? = copy()?.also { session.history.execute(RemoveLayer(selected)) }

        override fun paste(data: ClipData): Boolean {
            val custom = data.custom[LayerClip]
            val name = custom?.substringBefore('|') ?: data.text?.trim()?.takeIf { it.isNotEmpty() && '\n' !in it } ?: return false
            val shape = custom?.substringAfter('|')?.let(::decodeShape) ?: defaultShape
            val layers = session.history.document.value.layers
            val unique = generateSequence(name) { "$it copy" }.first { it !in layers }
            session.history.execute(AddLayer(unique, shape.copy(bounds = shape.bounds.translate(24f, 24f))))
            selected = unique
            return true
        }
    }
    val layersFocus = remember { FocusRequester() }
    val sceneFocus = remember { FocusRequester() }

    // A drawer offered only while the app wants it, to show drawers coming and going as it runs.
    var notesOffered by remember { mutableStateOf(false) }
    val notes = if (notesOffered) listOf(notesDrawer()) else emptyList()
    val drawers = notes + listOf(
        DrawerSpec("layers", "Layers", StretchIcons.Layers, count = doc.layers.size.toString(), keys = listOf("L")) {
            Column(
                Modifier
                    .padding(6.dp)
                    .focusRequester(layersFocus)
                    .clipboardTarget(router, layerClipboard)
                    .semantics { contentDescription = "Layer list" }
                    .focusable(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                for (layer in doc.layers) {
                    ListRow(layer == selected, { selected = layer; layersFocus.requestFocus() }) {
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
                        toasts.show("Deleted · $name", "Undo") { session.history.undo() }
                    }
                })
            }
        },
        DrawerSpec("library", "Library", StretchIcons.Grid) {
            DrawerSection(divider = false) { Tiles(listOf("Bean", "Drop", "Ear", "Paw", "Tail", "Snout"), it) }
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
        DrawerSpec("timeline", "Timeline", StretchIcons.Keyframe) {
            if (it == DrawerPosition.Bottom) {
                var frame by remember { mutableIntStateOf(0) }
                var playing by remember { mutableStateOf(false) }
                PlaybackEffect(playing, 24, 36, frame) { f -> frame = f }
                Timeline(36, frame, { f -> frame = f }, listOf(TimelineTrack("head", "Head turn", listOf(0, 12, 24)), TimelineTrack("tail", "Tail sway", listOf(0, 9, 18, 27))), playing, { p -> playing = p })
            } else {
                DrawerSection(divider = false) { Tiles((1..12).map { n -> "$n" }, it) }
            }
        },
        DrawerSpec("help", "Shortcuts", StretchIcons.Help) {
            KeymapEditor(keymap, registry.all.toList())
        },
        DrawerSpec("history", "History", StretchIcons.Undo) {
            HistoryPanel(history, ::stepLabel)
        },
        DrawerSpec("colors", "Colors", StretchIcons.Theme) {
            DrawerSection(divider = false) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for ((name, c) in listOf("Ink" to Color(0xFF1C1517), "Ochre" to Color(0xFFE9A24B), "Cream" to Color(0xFFF4EAD8))) {
                        Swatch(c, name, color == name, { color = name })
                    }
                }
                Button(if (notesOffered) "Take the notes drawer away" else "Offer a notes drawer", { notesOffered = !notesOffered })
            }
        },
    )

    StretchApp(
        layout = layouts[mode.name],
        onLayoutChange = { layouts = layouts.update(mode.name) { _ -> it } },
        drawers = drawers,
        onResetLayout = { layouts = layouts.reset(mode.name) },
        placeNewDrawer = { DrawerPlacement(it.id, Side.Right, open = true) },
        modifier = Modifier.actionKeys(keymap, mode.name),
        keyCast = if (showKeys) keyCast else null,
        topStrip = if (layerStrip) ({ LayerStrip(doc.layers, selected) }) else null,
        statusBar = {
            StatusBar(
                start = { StatusItem("${doc.layers.size} layers") },
                end = { StatusItem("${(camera.zoom * 100).roundToInt()}%", onClick = { camera.zoomTo(1f) }) },
            )
        },
        topBar = {
            bar(documentName(session), edited, {
                BarMenus(
                    listOf(
                        BarMenu("File") { close ->
                            ActionMenuItem(registry.named("new"), keymap, icons, close)
                            ActionMenuItem(registry.named("open"), keymap, icons, close)
                            ActionMenuItem(registry.named("save"), keymap, icons, close)
                            MenuDivider()
                            RecentFilesMenuItems(recent, documentStorage, ::open, close)
                        },
                        BarMenu("Edit") { close ->
                            ActionMenuItem(registry.named("undo"), keymap, icons, close)
                            ActionMenuItem(registry.named("redo"), keymap, icons, close)
                        },
                    ),
                )
            }, {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Segmented(Mode.entries, mode, { mode = it }, { it.name }, mode = true)
                    switcher()
                }
            }, { GoToField { palette = true } }) {
                TopBarActions(
                    onUndo = { history.undo() },
                    onRedo = { history.redo() },
                    onCommands = { palette = true },
                    onSettings = { settings = true },
                    more = listOf(
                        BarAction(StretchIcons.Folder, "Open", { fileMenu = true }, listOf("Ctrl", "O"), popupOpen = fileMenu, popup = {
                            Menu(fileMenu, { fileMenu = false }) {
                                MenuItem("New", { fileMenu = false; registry[ActionId("new")]?.run?.invoke() }, StretchIcons.File)
                                MenuItem("Open…", { fileMenu = false; chooseAndOpen() }, StretchIcons.Folder)
                                MenuDivider()
                                RecentFilesMenuItems(recent, documentStorage, ::open) { fileMenu = false }
                            }
                        }),
                        BarAction(StretchIcons.Save, "Save", { save() }, listOf("Ctrl", "S")),
                    ),
                    settingsOpen = settings,
                    commandsKeys = keymap.capsFor(registry.named("commands")),
                ) {
                    Popover(settings, { settings = false }, title = "Settings") {
                        SettingRow("Show keys") { Spacer(Modifier.weight(1f)); Toggle(showKeys, { showKeys = it }, "Show key presses") }
                        SettingRow("Rulers") { Spacer(Modifier.weight(1f)); Toggle(rulers, { rulers = it }, "Rulers") }
                        SettingRow("Layer strip") { Spacer(Modifier.weight(1f)); Toggle(layerStrip, { layerStrip = it }, "Layer strip") }
                        SettingRow("Size") {
                            Spacer(Modifier.weight(1f))
                            Segmented(listOf(0.9f, 1f, 1.25f, 1.5f), scale, onScale, { "${(it * 100).roundToInt()}%" })
                        }
                    }
                }
            }
        },
    ) {
        LaunchedEffect(camera.viewSize.isEmpty()) {
            if (!camera.viewSize.isEmpty()) camera.fit(sceneBounds(session.history.document.value))
        }
        var gesture by remember { mutableStateOf<History<DemoDoc>.Transaction?>(null) }
        Viewport(
            camera = camera,
            rulers = rulers,
            guides = guides,
            onGuidesChange = { guides = it },
            topStart = {
                Toolbar {
                    ToolButton(StretchIcons.Pen, "Draw", tool == "draw", { tool = "draw" }, size = 34.dp)
                    ToolButton(StretchIcons.Eraser, "Erase", tool == "erase", { tool = "erase" }, size = 34.dp)
                    ToolButton(StretchIcons.Mirror, "Mirror", tool == "mirror", { tool = "mirror" }, size = 34.dp)
                }
            },
            topEnd = { ViewNavigator(camera.zoom, { camera.zoomTo(it) }, { camera.fit(sceneBounds(doc)) }) },
            bottomStart = { Hud("$selected · ${formatNumber(doc.width)} px") },
            bottomCenter = { ToastHost(toasts) },
        ) {
            val palette6 = with(Stretch.colors) { listOf(chart1, chart2, chart3, chart4, chart5, chart6) }
            Canvas(
                Modifier
                    .fillMaxSize()
                    .focusRequester(sceneFocus)
                    .clipboardTarget(router, layerClipboard)
                    .semantics {
                        contentDescription = "Canvas"
                        stateDescription = "$selected selected"
                    }
                    .focusable()
                    .pointerInput(Unit) {
                        detectTapGestures { p ->
                            sceneFocus.requestFocus()
                            val at = camera.toScene(p)
                            val d = session.history.document.value
                            d.layers.lastOrNull { d.shapes[it]?.contains(at) == true }?.let { selected = it }
                        }
                    }
                    .marquee(camera) { area, _ ->
                        sceneFocus.requestFocus()
                        val d = session.history.document.value
                        d.layers.lastOrNull { d.shapes[it]?.bounds?.overlaps(area) == true }?.let { selected = it }
                    }
                    .transformHandles(
                        transform = doc.shapes[selected]?.takeIf { selected in doc.layers },
                        camera = camera,
                        onTransform = { t ->
                            val g = gesture ?: session.history.begin().also { gesture = it }
                            g.update(SetShape(selected, t))
                        },
                        onTransformFinished = {
                            gesture?.commit()
                            gesture = null
                        },
                    ),
            ) {
                for (layer in doc.layers) {
                    val shape = doc.shapes[layer] ?: continue
                    val box = camera.toScreen(shape.bounds)
                    rotate(shape.rotation, box.center) {
                        drawRoundRect(palette6[startingDoc.layers.indexOf(layer).takeIf { it >= 0 } ?: (layer.length % palette6.size)].copy(alpha = 0.55f), box.topLeft, box.size, CornerRadius(12f * camera.zoom))
                    }
                }
            }
        }
    }
    CommandPalette(palette, registry.available(mode.name).filter { it.id.value != "commands" }, keymap, { palette = false }, icons)
    UnsavedChangesDialog(session, chooser) { toasts.show("Couldn't save: ${it.message}") }
    if (recovery != null) {
        RecoveryDialog(
            recovered,
            onRestore = { item ->
                scope.launch {
                    runCatching { session.restore(DemoFormat.decode(recovery.read(item)), item.path) }
                    recovery.discard(item)
                    recovered = emptyList()
                }
            },
            onDiscard = { item ->
                scope.launch { recovery.discard(item) }
                recovered = recovered - item
            },
            onDismiss = { recovered = emptyList() },
        )
    }
}

@Composable
private fun Column6(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
}

/** Library and timeline cells: a wrapping grid in a side column, one scrolling strip in the bottom panel. */
@Composable
private fun Tiles(names: List<String>, position: DrawerPosition) {
    val c = Stretch.colors
    val tile = @Composable { name: String ->
        Box(
            Modifier.size(56.dp).clip(Stretch.shapes.cardShape).background(c.surfaceControl),
            contentAlignment = Alignment.Center,
        ) { BasicText(name, style = Stretch.type.bodySm.merge(TextStyle(color = c.ink))) }
    }
    when (position) {
        DrawerPosition.Side, DrawerPosition.Window -> Grid(minCell = 56.dp, gap = 6.dp) { names.forEach { tile(it) } }
        DrawerPosition.Bottom -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            names.forEach { tile(it) }
        }
    }
}
