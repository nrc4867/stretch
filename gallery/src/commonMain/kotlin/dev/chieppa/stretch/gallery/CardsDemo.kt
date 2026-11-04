package dev.chieppa.stretch.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.ContextMenuArea
import dev.chieppa.stretch.frame.MenuItem
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.AddCard
import dev.chieppa.stretch.ui.ArtCard
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.ButtonKind
import dev.chieppa.stretch.ui.CardSize
import dev.chieppa.stretch.ui.Chip
import dev.chieppa.stretch.ui.ChipKind

private val outline = Color(0xFF1C1517)
private val amber = Color(0xFFE9A24B)
private val ash = Color(0xFFA2A6A8)
private val rust = Color(0xFFC9603C)

private data class Shot(val name: String, val frames: String, val lean: Float)

private val shots = listOf(Shot("Shot 1", "24 f", 0f), Shot("Shot 2", "36 f", -0.1f), Shot("Shot 3", "40 f", -0.25f), Shot("Shot 4", "1 f", 0.2f))

/** The sizes a home screen mixes: one large card, a block of shots, a row of characters and a row to choose from. */
@Composable
internal fun CardsDemo() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LeadRow()
        CastRow()
        ChoiceRow()
    }
}

@Composable
private fun LeadRow() {
    Row(Modifier.fillMaxWidth().height(320.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ArtCard(
            "Fennec",
            Modifier.weight(1f).fillMaxHeight(),
            detail = "Build · 8 of 10 parts",
            size = CardSize.Large,
            trailing = { Button("Continue", {}, kind = ButtonKind.Primary) },
        ) { CritterArt(amber, "The fennec, as last left") }
        Column(Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            shots.chunked(2).forEach { row ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { ShotCard(it, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
    }
}

@Composable
private fun ShotCard(shot: Shot, modifier: Modifier) {
    ArtCard(
        shot.name,
        modifier,
        detail = shot.frames,
        onClick = {},
        trailing = if (shot.name == "Shot 3") ({ Chip("Last open", kind = ChipKind.On) }) else null,
    ) { SceneArt(shot) }
}

@Composable
private fun CastRow() {
    Row(Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ArtCard("Fennec", Modifier.weight(1f).fillMaxHeight(), detail = "v3 · edited", onClick = {}) { CritterArt(amber, "Fennec") }
        ContextMenuArea(
            menu = { close ->
                MenuItem("Open", { close() })
                MenuItem("Update from library", { close() })
                MenuItem("Remove from project", { close() })
            },
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            ArtCard("Ash fox", Modifier.fillMaxSize(), detail = "v3", badge = "Newer version in the library", onClick = {}) { CritterArt(ash, "Ash fox") }
        }
        ArtCard("Pup", Modifier.weight(1f).fillMaxHeight(), detail = "4 of 10", onClick = {}) { CritterArt(amber, "Pup", unfinished = true) }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AddCard("New character", {}, Modifier.weight(1f).fillMaxWidth())
            AddCard("From library", {}, Modifier.weight(1f).fillMaxWidth(), icon = StretchIcons.Import)
        }
    }
}

@Composable
private fun ChoiceRow() {
    val choices = listOf("Fennec" to amber, "Ash fox" to ash, "Rust fox" to rust)
    var picked by remember { mutableStateOf("Fennec") }
    Row(Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        choices.forEach { (name, color) ->
            ArtCard(name, Modifier.weight(1f).fillMaxHeight(), detail = "v3", selected = picked == name, onClick = { picked = name }) {
                CritterArt(color, name)
            }
        }
    }
}

@Composable
private fun CritterArt(fur: Color, description: String, unfinished: Boolean = false) {
    Canvas(Modifier.fillMaxSize().semantics { contentDescription = description }) {
        val r = minOf(size.width, size.height) * 0.36f
        drawCritter(Offset(size.width / 2, size.height / 2 + r * 0.35f), r, fur, unfinished)
    }
}

@Composable
private fun SceneArt(shot: Shot) {
    Canvas(Modifier.fillMaxSize().semantics { contentDescription = shot.name }) {
        val horizon = size.height * 0.68f
        drawRect(Color(0xFFBCD2DC), size = Size(size.width, horizon))
        val hills = Path().apply {
            moveTo(0f, horizon)
            cubicTo(size.width * 0.25f, horizon * 0.55f, size.width * 0.6f, horizon * 0.75f, size.width, horizon * 0.7f)
            lineTo(size.width, horizon)
            close()
        }
        drawPath(hills, Color(0xFF8FAFA6))
        drawRect(Color(0xFFC7BD9C), topLeft = Offset(0f, horizon), size = Size(size.width, size.height - horizon))
        val r = size.height * 0.3f
        drawCritter(Offset(size.width * (0.5f + shot.lean), horizon - r * 0.3f), r, amber, unfinished = false)
    }
}

/** A round body and head with pointed ears; [unfinished] leaves the body as a dashed outline. */
private fun DrawScope.drawCritter(center: Offset, radius: Float, fur: Color, unfinished: Boolean) {
    val line = radius * 0.05f
    val head = Offset(center.x - radius * 0.55f, center.y - radius * 0.45f)
    val headR = radius * 0.5f
    val body = Offset(center.x + radius * 0.2f, center.y + radius * 0.25f)
    val bodyR = radius * 0.65f
    if (unfinished) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(line * 3, line * 2))
        drawCircle(outline.copy(alpha = 0.6f), bodyR, body, style = Stroke(line, pathEffect = dash))
    } else {
        drawCircle(fur, bodyR, body)
        drawCircle(outline, bodyR, body, style = Stroke(line))
    }
    for (side in listOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(head.x + side * headR * 0.2f, head.y - headR * 0.7f)
            lineTo(head.x + side * headR * 0.8f, head.y - headR * 1.7f)
            lineTo(head.x + side * headR * 0.95f, head.y - headR * 0.35f)
            close()
        }
        drawPath(ear, fur)
        drawPath(ear, outline, style = Stroke(line))
    }
    drawCircle(fur, headR, head)
    drawCircle(outline, headR, head, style = Stroke(line))
    drawCircle(outline, headR * 0.12f, Offset(head.x - headR * 0.35f, head.y - headR * 0.1f))
}
