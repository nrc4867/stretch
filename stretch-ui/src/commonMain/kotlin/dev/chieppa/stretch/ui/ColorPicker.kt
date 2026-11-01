package dev.chieppa.stretch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import kotlin.math.abs
import kotlin.math.roundToInt

/** A color as hue (0 to 360), saturation and value (0 to 1). */
@Immutable
data class Hsv(val h: Float, val s: Float, val v: Float) {
    fun toColor(alpha: Float = 1f): Color {
        val c = v * s
        val hh = ((h % 360f) + 360f) % 360f / 60f
        val x = c * (1 - abs(hh % 2 - 1))
        val (r, g, b) = when (hh.toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = v - c
        return Color(r + m, g + m, b + m, alpha)
    }
}

fun Color.toHsv(): Hsv {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val d = max - min
    val h = when {
        d == 0f -> 0f
        max == red -> 60f * (((green - blue) / d) % 6f)
        max == green -> 60f * ((blue - red) / d + 2f)
        else -> 60f * ((red - green) / d + 4f)
    }
    return Hsv((h + 360f) % 360f, if (max == 0f) 0f else d / max, max)
}

/** "#E9A24B" for an opaque color. */
fun Color.toHex(): String {
    fun part(f: Float) = (f * 255).roundToInt().coerceIn(0, 255).toString(16).padStart(2, '0')
    return "#" + (part(red) + part(green) + part(blue)).uppercase()
}

/** Parses "#e9a24b", "e9a24b" or "#ea4"; null when it is not a color. */
fun parseHex(text: String): Color? {
    val t = text.trim().removePrefix("#")
    val full = when (t.length) {
        3 -> t.map { "$it$it" }.joinToString("")
        6 -> t
        else -> return null
    }
    val n = full.toLongOrNull(16) ?: return null
    return Color(((n shr 16) and 0xFF) / 255f, ((n shr 8) and 0xFF) / 255f, (n and 0xFF) / 255f)
}

/**
 * A color picker: a saturation and value square, a hue strip, the hex value and recent colors. [onPickFromScreen]
 * adds an eyedropper button; picking from the viewport is the app's job. Arrow keys nudge the focused square or strip
 * (Shift for ten steps). [onChangeFinished] marks the end of a drag or an edit, for one undo step.
 */
@Composable
fun ColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    recent: List<Color> = emptyList(),
    onPickFromScreen: (() -> Unit)? = null,
    onChangeFinished: () -> Unit = {},
    hexDescription: String = "Hex",
    squareDescription: String = "Saturation and brightness",
    hueDescription: String = "Hue",
    eyedropperDescription: String = "Pick from screen",
    invalidHex: String = "Not a hex color",
) {
    val c = Stretch.colors
    val incoming = color.toHsv()
    var kept by remember { mutableFloatStateOf(incoming.h) }
    val hue = if (incoming.s > 0.001f && incoming.v > 0.001f) incoming.h else kept
    val hsv = incoming.copy(h = hue)
    val change by rememberUpdatedState(onColorChange)
    val finished by rememberUpdatedState(onChangeFinished)
    val current by rememberUpdatedState(hsv)
    val shape = RoundedCornerShape(Stretch.shapes.ctl)
    fun set(next: Hsv) {
        kept = next.h
        change(next.toColor(color.alpha))
    }
    fun nudge(event: androidx.compose.ui.input.key.KeyEvent, dx: Float, dy: Float, apply: (Float, Float) -> Hsv): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        val step = if (event.isShiftPressed) 10f else 1f
        val (x, y) = when (event.key) {
            Key.DirectionLeft -> -step * dx to 0f
            Key.DirectionRight -> step * dx to 0f
            Key.DirectionUp -> 0f to step * dy
            Key.DirectionDown -> 0f to -step * dy
            else -> return false
        }
        set(apply(x, y))
        finished()
        return true
    }

    Column(modifier.width(240.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val square = rememberControlState()
        val squareFocus = remember { FocusRequester() }
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .focusRing(square, shape)
                .clip(shape)
                .border(Stretch.stroke, c.lineControl, shape)
                .focusRequester(squareFocus)
                .focusable(interactionSource = square.source)
                .onKeyEvent { e -> nudge(e, 0.01f, 0.01f) { x, y -> current.copy(s = (current.s + x).coerceIn(0f, 1f), v = (current.v + y).coerceIn(0f, 1f)) } }
                .semantics {
                    contentDescription = squareDescription
                    stateDescription = "${(hsv.s * 100).roundToInt()}%, ${(hsv.v * 100).roundToInt()}%"
                }
                .pointerInput(Unit) {
                    fun at(p: Offset) = set(current.copy(s = (p.x / size.width).coerceIn(0f, 1f), v = (1 - p.y / size.height).coerceIn(0f, 1f)))
                    detectTapGestures(onPress = { squareFocus.requestFocus(); at(it); tryAwaitRelease(); finished() })
                }
                .pointerInput(Unit) {
                    fun at(p: Offset) = set(current.copy(s = (p.x / size.width).coerceIn(0f, 1f), v = (1 - p.y / size.height).coerceIn(0f, 1f)))
                    detectDragGestures(onDragEnd = { finished() }) { change, _ -> change.consume(); at(change.position) }
                },
        ) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, Hsv(hue, 1f, 1f).toColor())))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            val p = Offset(hsv.s * size.width, (1 - hsv.v) * size.height)
            drawCircle(Color.White, 7.dp.toPx(), p, style = Stroke(2.5.dp.toPx()))
            drawCircle(Color.Black, 8.5.dp.toPx(), p, style = Stroke(1.dp.toPx()))
        }
        val strip = rememberControlState()
        val stripFocus = remember { FocusRequester() }
        val pill = Stretch.shapes.pillShape
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .focusRing(strip, pill)
                .clip(pill)
                .focusRequester(stripFocus)
                .focusable(interactionSource = strip.source)
                .onKeyEvent { e -> nudge(e, 1f, 1f) { x, y -> current.copy(h = ((current.h + x + y) % 360f + 360f) % 360f) } }
                .semantics {
                    contentDescription = hueDescription
                    stateDescription = "${hue.roundToInt()}°"
                }
                .pointerInput(Unit) {
                    detectTapGestures(onPress = { stripFocus.requestFocus(); set(current.copy(h = (it.x / size.width * 360f).coerceIn(0f, 359.9f))); tryAwaitRelease(); finished() })
                }
                .pointerInput(Unit) {
                    detectDragGestures(onDragEnd = { finished() }) { change, _ ->
                        change.consume()
                        set(current.copy(h = (change.position.x / size.width * 360f).coerceIn(0f, 359.9f)))
                    }
                },
        ) {
            drawRect(Brush.horizontalGradient((0..6).map { Hsv(it * 60f, 1f, 1f).toColor() }))
            val x = hue / 360f * size.width
            drawRoundRect(Color.White, Offset(x - 3.dp.toPx(), 1.dp.toPx()), androidx.compose.ui.geometry.Size(6.dp.toPx(), size.height - 2.dp.toPx()), CornerRadius(3.dp.toPx()), style = Stroke(2.dp.toPx()))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            var text by remember(color) { mutableStateOf(color.toHex()) }
            val parsed = parseHex(text)
            TextField(
                text,
                {
                    text = it
                    parseHex(it)?.let { p -> change(p.copy(alpha = color.alpha)); p.toHsv().takeIf { h -> h.s > 0f }?.let { h -> kept = h.h } }
                },
                Modifier.weight(1f),
                placeholder = "#000000",
                contentDescription = hexDescription,
                leading = { Canvas(Modifier.size(14.dp).clip(Stretch.shapes.segShape)) { drawRect(color) } },
                error = if (parsed == null) invalidHex else null,
            )
            if (onPickFromScreen != null) IconButton(StretchIcons.Eyedropper, eyedropperDescription, onPickFromScreen, quiet = false)
        }
        if (recent.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (r in recent.take(8)) Swatch(r, r.toHex(), r.toHex() == color.toHex(), { set(r.toHsv()); finished() })
            }
        }
    }
}
