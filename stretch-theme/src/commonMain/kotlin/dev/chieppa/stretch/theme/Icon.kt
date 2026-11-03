package dev.chieppa.stretch.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal class IconScope(private val builder: ImageVector.Builder) {
    fun stroke(d: String) {
        builder.addPath(
            pathData = addPathNodes(d),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }

    fun fill(d: String) {
        builder.addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
    }
}

/** Icons that point back or forward, and so face the other way in a right-to-left layout. */
private val directional = setOf("undo", "redo", "chevron-left", "chevron-right")

internal fun icon(name: String, block: IconScope.() -> Unit): ImageVector {
    val builder = ImageVector.Builder(name, 16.dp, 16.dp, 16f, 16f, autoMirror = name in directional)
    IconScope(builder).block()
    return builder.build()
}

/** A Stretch icon tinted with [tint]. Decorative unless [contentDescription] is given. */
@Composable
fun Icon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    contentDescription: String? = null,
) {
    Image(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
