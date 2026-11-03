package dev.chieppa.stretch.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.BarChart
import dev.chieppa.stretch.ui.ChartSeries
import dev.chieppa.stretch.ui.Chip
import dev.chieppa.stretch.ui.Delta
import dev.chieppa.stretch.ui.Dropzone
import dev.chieppa.stretch.ui.LineChart
import dev.chieppa.stretch.ui.Segmented
import dev.chieppa.stretch.ui.SortableGrid
import dev.chieppa.stretch.ui.SortableList
import dev.chieppa.stretch.ui.Stat
import dev.chieppa.stretch.ui.Table
import dev.chieppa.stretch.ui.TableColumn
import dev.chieppa.stretch.ui.TableSort
import dev.chieppa.stretch.ui.compactNumber
import dev.chieppa.stretch.ui.groupThousands
import dev.chieppa.stretch.ui.moved
import dev.chieppa.stretch.ui.pickFiles
import java.io.File

private val images = setOf("png", "jpg", "jpeg", "svg")

private data class Part(val name: String, val type: String, val size: Int, val strokes: Int)

private val parts = listOf(
    Part("Body", "Torso", 420, 1284),
    Part("Front paws", "Paw · ×2", 64, 312),
    Part("Head", "Head", 180, 905),
    Part("Tail", "Tail", 240, 96),
)

private val partColumns = listOf(
    TableColumn<Part>("Part", weight = 1.4f, sortBy = { it.name }) { it.name },
    TableColumn<Part>("Type", sortBy = { it.type }) { it.type },
    TableColumn<Part>("Size (px)", numeric = true, sortBy = { it.size }) { it.size.toString() },
    TableColumn<Part>("Strokes", numeric = true, sortBy = { it.strokes }) { groupThousands(it.strokes.toLong()) },
)

@Composable
internal fun DataPage() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Section("Cards") { CardsDemo() }
        Section("Dropzone") { DropDemo() }
        Section("Drag and drop") { SortDemo() }
        Section("Stats") { StatsDemo() }
        Section("Charts") { ChartsDemo() }
        Section("Table") { TableDemo() }
    }
}

@Composable
private fun DropDemo() {
    var files by remember { mutableStateOf(listOf<String>()) }
    Dropzone(
        "Drop reference images", "PNG, JPG, SVG", images,
        onFiles = { files = files + it },
        onBrowse = { files = files + pickFiles("Import reference images", images) },
        overLabel = "Release to import",
        rejectLabel = "Not an image",
        modifier = Modifier.fillMaxWidth(),
    )
    if (files.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (f in files) Chip(File(f).name)
        }
    }
}

@Composable
private fun SortDemo() {
    var layers by remember { mutableStateOf(listOf("Body", "Head", "Tail", "Fur edge", "Front paws")) }
    var plain by remember { mutableStateOf(listOf("Ink", "Shade", "Light", "Rim")) }
    var poses by remember { mutableStateOf((1..6).map { "Pose $it" }) }
    var picked by remember { mutableStateOf("Head") }
    val text = TextStyle(color = Stretch.colors.ink).merge(Stretch.type.bodySm)
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        SortableList(layers, { it }, { from, to -> layers = layers.moved(from, to) }, Modifier.width(220.dp), onClick = { picked = it }) {
            BasicText(if (it == picked) "$it ·" else it, style = text)
        }
        SortableList(plain, { it }, { from, to -> plain = plain.moved(from, to) }, Modifier.width(160.dp), handles = false) {
            BasicText(it, style = text)
        }
        SortableGrid(poses, { it }, columns = 3, onMove = { from, to -> poses = poses.moved(from, to) }) {
            BasicText(it, style = text)
        }
    }
}

@Composable
private fun StatsDemo() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Stat("Parts drawn", compactNumber(1284.0), delta = Delta("+12% vs last week", good = true))
        Stat("Render time", "4.2 s", delta = Delta("+0.8 s vs last build", good = false))
        Stat("Strokes", compactNumber(12_900.0), trend = listOf(12f, 14f, 13f, 17f, 16f, 19f, 18f, 22f, 21f, 24f, 23f, 27f), trendDescription = "Strokes over 12 days, rising")
    }
}

@Composable
private fun ChartsDemo() {
    var table by remember { mutableStateOf(false) }
    Segmented(listOf(false, true), table, { table = it }, { if (it) "Table" else "Chart" })
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LineChart(
            "Strokes per day",
            listOf(
                ChartSeries("Front paws", listOf(8f, 10f, 9f, 13f, 15f, 14f, 18f, 21f), 0),
                ChartSeries("Head", listOf(5f, 6f, 8f, 7f, 9f, 12f, 11f, 13f), 1),
            ),
            (1..8).map { "Day $it" },
            Modifier.width(340.dp),
            description = "Strokes per day for front paws and head over 8 days",
            table = table,
        )
        BarChart(
            "Parts by region",
            listOf("Body" to 42f, "Head" to 31f, "Tail" to 18f, "Paws" to 27f),
            Modifier.width(300.dp),
            seriesName = "Parts",
            description = "Parts by region: body 42, head 31, tail 18, paws 27",
            table = table,
        )
    }
}

@Composable
private fun TableDemo() {
    var sort by remember { mutableStateOf<TableSort?>(TableSort(0)) }
    var selected by remember { mutableStateOf(parts[1]) }
    var dense by remember { mutableStateOf(false) }
    Segmented(listOf(false, true), dense, { dense = it }, { if (it) "Dense" else "Regular" })
    Table(
        parts, partColumns, Modifier.width(560.dp),
        sort = sort, onSortChange = { sort = it },
        isSelected = { it == selected }, onRowClick = { selected = it },
        dense = dense,
    )
}
