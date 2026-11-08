package dev.chieppa.stretch.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSpacing

/** The side of one square of the checkerboard behind a picture. */
private val CHECKER = 8.dp

/**
 * One picture, centred on a checkerboard that shows through its transparent parts. A picture larger than the view
 * shrinks to fit it, and a click shows it at its own size, scrolling both ways, until another click fits it again; a
 * smaller one shows at its own size, never enlarged. [contentDescription] names it for assistive technology.
 */
@Composable
fun ImageView(image: ImageBitmap, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val c = Stretch.colors
    val density = LocalDensity.current
    val natural = with(density) { DpSize(image.width.toDp(), image.height.toDp()) }
    var actual by remember(image) { mutableStateOf(false) }
    BoxWithConstraints(modifier.background(c.surfacePanel)) {
        val room = DpSize(maxWidth - StretchSpacing.space16 * 2, maxHeight - StretchSpacing.space16 * 2)
        val scale = fitScale(natural, room)
        val shrunk = scale < 1f
        val toggle = if (shrunk) Modifier.controlPointer().clickable(role = Role.Button) { actual = !actual } else Modifier
        if (actual && shrunk) {
            Box(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()).then(toggle),
            ) {
                Box(Modifier.padding(StretchSpacing.space16)) { Picture(image, natural, contentDescription, FilterQuality.None) }
            }
        } else {
            Box(Modifier.fillMaxSize().then(toggle), contentAlignment = Alignment.Center) {
                val size = DpSize(natural.width * scale, natural.height * scale)
                Picture(image, size, contentDescription, if (shrunk) FilterQuality.Medium else FilterQuality.None)
            }
        }
    }
}

/** How much a picture of [natural] size shrinks to fit [room], 1 when it fits already. */
internal fun fitScale(natural: DpSize, room: DpSize): Float {
    if (natural.width <= 0.dp || natural.height <= 0.dp) return 1f
    val across = (room.width / natural.width).coerceAtLeast(0f)
    val down = (room.height / natural.height).coerceAtLeast(0f)
    return minOf(1f, across, down)
}

@Composable
private fun Picture(image: ImageBitmap, size: DpSize, contentDescription: String?, quality: FilterQuality) {
    val c = Stretch.colors
    Image(
        image,
        contentDescription,
        Modifier
            .size(size)
            .border(Stretch.stroke, c.linePanel)
            .checkerboard(c.surfacePanel, c.surfaceControl, CHECKER),
        contentScale = ContentScale.FillBounds,
        filterQuality = quality,
    )
}

/** Squares of [light] and [dark], [square] on a side, behind the content. */
private fun Modifier.checkerboard(light: Color, dark: Color, square: Dp): Modifier = drawBehind {
    val side = square.toPx()
    drawRect(light)
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) 0f else side
        while (x < size.width) {
            drawRect(dark, Offset(x, y), Size(minOf(side, size.width - x), minOf(side, size.height - y)))
            x += side * 2
        }
        y += side
        row++
    }
}
