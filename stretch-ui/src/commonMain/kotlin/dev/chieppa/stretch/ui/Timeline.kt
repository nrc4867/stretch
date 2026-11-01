package dev.chieppa.stretch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import kotlin.math.floor
import kotlin.math.roundToInt

/** One timeline row: what it animates and the frames that hold a key. */
@Immutable
data class TimelineTrack(val id: String, val label: String, val keys: List<Int>)

/** A keyframe on a track. */
@Immutable
data class TimelineKey(val track: String, val frame: Int)

/** The frame under [x] on a ruler where each frame is [frameWidth] wide, within 0 until [frames]. */
fun frameAt(x: Float, frameWidth: Float, frames: Int): Int = floor(x / frameWidth).toInt().coerceIn(0, frames - 1)

/** How often the ruler labels a frame so labels stay at least [minGap] apart: 1, 2, 5, 10, 20, 50 ... */
fun rulerStep(frameWidth: Float, minGap: Float): Int {
    var step = 1
    val steps = intArrayOf(1, 2, 5)
    var scale = 1
    while (true) {
        for (s in steps) {
            step = s * scale
            if (step * frameWidth >= minGap) return step
        }
        scale *= 10
    }
}

/**
 * Advances [current] at [fps] while [playing], looping at [frames]. The timeline shows time; this keeps it moving,
 * so the app decides when playback runs.
 */
@Composable
fun PlaybackEffect(playing: Boolean, fps: Int, frames: Int, current: Int, onCurrentChange: (Int) -> Unit) {
    val frame by rememberUpdatedState(current)
    val change by rememberUpdatedState(onCurrentChange)
    LaunchedEffect(playing, fps, frames) {
        if (!playing) return@LaunchedEffect
        var last = withFrameNanos { it }
        var carry = 0L
        val per = 1_000_000_000L / fps
        while (true) {
            val now = withFrameNanos { it }
            carry += now - last
            last = now
            if (carry >= per) {
                val steps = (carry / per).toInt()
                carry %= per
                change((frame + steps) % frames)
            }
        }
    }
}

/**
 * The timeline for animation, made for the bottom panel: transport (play, step, the frame and the rate), a frame
 * ruler, one row per track with its keys, and the playhead in the accent. Click or drag the ruler to scrub; click a
 * key to select it (Shift adds), drag selected keys to retime them. Left and Right step a frame, Home and End jump,
 * Space plays. Selection and keys are the app's state.
 */
@Composable
fun Timeline(
    frames: Int,
    current: Int,
    onCurrentChange: (Int) -> Unit,
    tracks: List<TimelineTrack>,
    playing: Boolean,
    onPlayingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    fps: Int = 24,
    selected: Set<TimelineKey> = emptySet(),
    onSelectedChange: (Set<TimelineKey>) -> Unit = {},
    onMoveKeys: ((Set<TimelineKey>, delta: Int) -> Unit)? = null,
    frameWidth: Dp = 14.dp,
    labels: TimelineLabels = TimelineLabels(),
) {
    val c = Stretch.colors
    val density = LocalDensity.current
    val fw = with(density) { frameWidth.toPx() }
    val now by rememberUpdatedState(current)
    val change by rememberUpdatedState(onCurrentChange)
    val currentSelection by rememberUpdatedState(selected)
    val select by rememberUpdatedState(onSelectedChange)
    val move by rememberUpdatedState(onMoveKeys)
    val focus = remember { FocusRequester() }
    var preview by remember { mutableIntStateOf(0) }
    val measurer = rememberTextMeasurer()
    val tick = Stretch.type.value.copy(fontSize = 10.sp, color = c.inkMuted)
    val step = rulerStep(fw, with(density) { 28.dp.toPx() })
    val rowHeight = 24.dp
    val rulerHeight = 22.dp
    val scroll = rememberScrollState()

    Column(
        modifier
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                val by = if (e.isShiftPressed) 10 else 1
                when (e.key) {
                    Key.DirectionRight -> change((now + by).coerceAtMost(frames - 1))
                    Key.DirectionLeft -> change((now - by).coerceAtLeast(0))
                    Key.MoveHome -> change(0)
                    Key.MoveEnd -> change(frames - 1)
                    Key.Spacebar -> onPlayingChange(!playing)
                    else -> return@onKeyEvent false
                }
                true
            },
    ) {
        Row(
            Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(StretchIcons.StepBack, labels.stepBack, { change((now - 1).coerceAtLeast(0)) })
            IconButton(if (playing) StretchIcons.Pause else StretchIcons.Play, if (playing) labels.pause else labels.play, { onPlayingChange(!playing) }, on = playing)
            IconButton(StretchIcons.StepForward, labels.stepForward, { change((now + 1).coerceAtMost(frames - 1)) })
            Label("${current + 1} / $frames · $fps ${labels.fps}", Stretch.type.value, c.inkMuted, Modifier.padding(start = 8.dp))
        }
        Box(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.width(120.dp)) {
                Box(Modifier.height(rulerHeight))
                for (t in tracks) Label(t.label, Stretch.type.bodySm, c.ink, Modifier.height(rowHeight).padding(start = 12.dp, top = 4.dp))
            }
            Box(Modifier.weight(1f).horizontalScroll(scroll)) {
                val width = frameWidth * frames
                Column(Modifier.width(width)) {
                    Canvas(
                        Modifier
                            .width(width)
                            .height(rulerHeight)
                            .semantics {
                                contentDescription = labels.ruler
                                stateDescription = "${current + 1}"
                            }
                            .pointerInput(frames, fw) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    focus.requestFocus()
                                    change(frameAt(down.position.x, fw, frames))
                                    drag(down.id) { ch ->
                                        ch.consume()
                                        change(frameAt(ch.position.x, fw, frames))
                                    }
                                }
                            },
                    ) {
                        drawRect(c.surfaceRail)
                        for (f in 0 until frames) {
                            val x = f * fw
                            val major = f % step == 0
                            drawLine(if (major) c.chartAxis else c.chartGrid, Offset(x, size.height), Offset(x, size.height - if (major) 8.dp.toPx() else 4.dp.toPx()), 1f)
                            if (major) drawText(measurer.measure("${f + 1}", tick), topLeft = Offset(x + 2.dp.toPx(), 1.dp.toPx()))
                        }
                        drawLine(c.linePanel, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1f)
                    }
                    for (t in tracks) {
                        Canvas(
                            Modifier
                                .width(width)
                                .height(rowHeight)
                                .semantics { contentDescription = "${t.label}: ${t.keys.joinToString { "${it + 1}" }}" }
                                .pointerInput(t, fw) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown()
                                        focus.requestFocus()
                                        val frame = frameAt(down.position.x, fw, frames)
                                        val hit = t.keys.firstOrNull { it == frame }
                                        if (hit == null) {
                                            if (!currentEvent.keyboardModifiers.isShiftPressed) select(emptySet())
                                            change(frame)
                                            return@awaitEachGesture
                                        }
                                        val key = TimelineKey(t.id, hit)
                                        val sel = when {
                                            currentEvent.keyboardModifiers.isShiftPressed -> if (key in currentSelection) currentSelection - key else currentSelection + key
                                            key in currentSelection -> currentSelection
                                            else -> setOf(key)
                                        }
                                        select(sel)
                                        val m = move ?: return@awaitEachGesture
                                        var delta = 0
                                        drag(down.id) { ch ->
                                            ch.consume()
                                            delta = frameAt(ch.position.x, fw, frames) - frame
                                            preview = delta
                                        }
                                        preview = 0
                                        if (delta != 0 && key in sel) m(sel, delta)
                                    }
                                },
                        ) {
                            for (f in 0 until frames step step) drawLine(c.chartGrid, Offset(f * fw, 0f), Offset(f * fw, size.height), 1f)
                            drawLine(c.chartGrid, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1f)
                            for (k in t.keys) {
                                val isSel = TimelineKey(t.id, k) in selected
                                val f = if (isSel) k + preview else k
                                val cx = f * fw + fw / 2
                                val cy = size.height / 2
                                val r = 5.dp.toPx()
                                val diamond = Path().apply {
                                    moveTo(cx, cy - r)
                                    lineTo(cx + r, cy)
                                    lineTo(cx, cy + r)
                                    lineTo(cx - r, cy)
                                    close()
                                }
                                drawPath(diamond, if (isSel) c.onFill else c.surfaceFloat)
                                drawPath(diamond, if (isSel) c.onEdge else c.inkStrong, style = Stroke(1.5.dp.toPx()))
                            }
                        }
                    }
                }
                Canvas(Modifier.width(width).height(rulerHeight + rowHeight * tracks.size)) {
                    val x = current * fw + fw / 2
                    drawLine(c.accent, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
                    val head = Path().apply {
                        moveTo(x - 5.dp.toPx(), 0f)
                        lineTo(x + 5.dp.toPx(), 0f)
                        lineTo(x + 5.dp.toPx(), 8.dp.toPx())
                        lineTo(x, 12.dp.toPx())
                        lineTo(x - 5.dp.toPx(), 8.dp.toPx())
                        close()
                    }
                    drawPath(head, c.accent)
                }
            }
        }
        LaunchedEffect(current) {
            val x = (current * fw).roundToInt()
            if (x < scroll.value || x > scroll.value + scroll.viewportSize - fw) scroll.animateScrollTo((x - scroll.viewportSize / 2).coerceAtLeast(0))
        }
    }
}

/** The words a [Timeline] shows and speaks; apps pass their own strings. */
@Immutable
data class TimelineLabels(
    val play: String = "Play",
    val pause: String = "Pause",
    val stepBack: String = "Previous frame",
    val stepForward: String = "Next frame",
    val fps: String = "fps",
    val ruler: String = "Frame",
)
