package dev.chieppa.stretch.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM
import org.jetbrains.skia.svg.SVGLength
import org.jetbrains.skia.svg.SVGLengthUnit
import kotlin.math.ceil

/** How much of a file is looked at for an SVG's opening tag. */
private const val SVG_PROBE = 1024

/** The size an SVG with no size of its own is drawn at, in pixels. */
private const val SVG_FALLBACK = 256f

/** The largest side an SVG is drawn at, in pixels. */
private const val SVG_LARGEST = 4096f

actual fun decodeImage(bytes: ByteArray): ImageBitmap? {
    if (bytes.isEmpty()) return null
    return if (looksLikeSvg(bytes)) svg(bytes) else raster(bytes)
}

private fun raster(bytes: ByteArray): ImageBitmap? =
    runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

private fun looksLikeSvg(bytes: ByteArray): Boolean =
    bytes.copyOf(minOf(bytes.size, SVG_PROBE)).decodeToString().contains("<svg")

private fun svg(bytes: ByteArray): ImageBitmap? = runCatching {
    val dom = SVGDOM(Data.makeFromBytes(bytes))
    val root = dom.root ?: return null
    val box = root.viewBox
    var width = pixels(root.width)
    var height = pixels(root.height)
    if (width <= 0f || height <= 0f) {
        width = box?.width?.takeIf { it > 0f } ?: SVG_FALLBACK
        height = box?.height?.takeIf { it > 0f } ?: SVG_FALLBACK
    }
    val scale = minOf(1f, SVG_LARGEST / maxOf(width, height))
    val w = ceil(width * scale).toInt()
    val h = ceil(height * scale).toInt()
    dom.setContainerSize(w.toFloat(), h.toFloat())
    val surface = Surface.makeRasterN32Premul(w, h)
    dom.render(surface.canvas)
    surface.makeImageSnapshot().toComposeImageBitmap()
}.getOrNull()

/** A length in pixels, or 0 for one in other units, such as a percentage of a container an SVG file does not have. */
private fun pixels(length: SVGLength): Float =
    if (length.unit == SVGLengthUnit.NUMBER || length.unit == SVGLengthUnit.PX) length.value else 0f
