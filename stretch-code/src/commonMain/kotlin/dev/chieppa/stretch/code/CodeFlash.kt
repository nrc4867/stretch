package dev.chieppa.stretch.code

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

/**
 * A run on one line of [group] to wash for a moment: [start] until [end] on [line], columns before tabs expand, or the
 * whole line when [end] is null. [serial] tells two flashes of the same run apart, so flashing it again starts over.
 */
data class CodeFlashSpot<G>(val group: G, val line: Int, val start: Int, val end: Int?, val serial: Int)

/**
 * A short wash over a place a view has just gone to, so the eye finds it: it shows at once in the accent and fades out
 * over [FADE_MS]. Only one flashes at a time; a new one replaces the last. A view runs [run] while it shows and draws
 * the wash with [codeFlash]; [CodeViewer] and the diff viewer keep one in their state, and other code views can too.
 */
@Stable
class CodeFlash<G> {
    var spot by mutableStateOf<CodeFlashSpot<G>?>(null)
        private set

    /** How strongly the wash shows, from 1 as it starts to 0 once it has faded. */
    val strength = Animatable(0f)

    private var serial = 0

    fun start(group: G, line: Int, start: Int, end: Int?) {
        spot = CodeFlashSpot(group, line, start, end, ++serial)
    }

    /** Fades each flash out as it starts, on the view's frame clock, and forgets it once it has faded. */
    suspend fun run() {
        snapshotFlow { spot }.collectLatest { flashing ->
            flashing ?: return@collectLatest
            strength.snapTo(1f)
            strength.animateTo(0f, tween(FADE_MS, delayMillis = HOLD_MS))
            if (spot == flashing) spot = null
        }
    }

    companion object {
        /** How long the wash shows at full strength before it fades. */
        const val HOLD_MS = 250

        /** How long the wash takes to fade out. */
        const val FADE_MS = 900
    }
}

/** How strong the accent is at a flash's start, so the code stays readable through it. */
private const val FLASH_ALPHA = 0.45f

/** How round a flash's corners are. */
private val FLASH_CORNER = 2.dp

/**
 * Washes the flash in [flash] behind this line's code while it is on one of [keys], in [color]: over its run, found
 * through the line's [geometry], or behind the whole code for a whole line.
 */
fun <G> Modifier.codeFlash(flash: CodeFlash<G>, keys: List<CodeLineKey<G>>, geometry: CodeLineGeometry, color: Color): Modifier = drawBehind {
    val spot = flash.spot ?: return@drawBehind
    if (keys.none { it.group == spot.group && it.line == spot.line }) return@drawBehind
    val strength = flash.strength.value
    if (strength <= 0f) return@drawBehind
    val layout = geometry.layout ?: return@drawBehind
    val wash = color.copy(alpha = color.alpha * FLASH_ALPHA * strength)
    val corner = CornerRadius(FLASH_CORNER.toPx())
    val end = spot.end
    if (end == null) {
        drawRoundRect(wash, Offset.Zero, size, corner)
        return@drawBehind
    }
    for (box in geometry.rowBoxes(spot.start, end, layout)) drawRoundRect(wash, box.topLeft, box.size, corner)
}
