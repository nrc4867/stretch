package dev.chieppa.stretch.gpu.readback

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.lang.foreign.MemorySegment
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val BYTES_PER_PIXEL = 4

private fun bgraInfo(width: Int, height: Int) = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.PREMUL)

/**
 * The bytes a [width] by [height] image with rows [rowBytes] apart takes, as Skia allocates it: every row but the last
 * is padded to [rowBytes], and the last ends at its last pixel.
 */
internal fun frameByteSize(width: Int, height: Int, rowBytes: Int): Long = rowBytes.toLong() * (height - 1) + width.toLong() * BYTES_PER_PIXEL

/**
 * A [width] by [height] BGRA image whose rows sit [rowBytes] apart, filled by [fill] writing straight into its pixel
 * memory, [frameByteSize] bytes long. The bitmap is made immutable before the image is made from it, so the image
 * shares its pixels rather than copying them.
 */
internal fun bgraFrameImage(width: Int, height: Int, rowBytes: Int, fill: (MemorySegment) -> Unit): Pair<Bitmap, Image> {
    val bitmap = Bitmap()
    check(bitmap.allocPixels(bgraInfo(width, height), rowBytes)) { "Could not allocate a $width by $height frame" }
    val pixmap = checkNotNull(bitmap.peekPixels())
    try {
        fill(MemorySegment.ofAddress(pixmap.addr).reinterpret(frameByteSize(width, height, rowBytes)))
    } finally {
        pixmap.close()
    }
    bitmap.setImmutable()
    return bitmap to Image.makeFromBitmap(bitmap)
}

/** An image of packed ARGB [pixels], the obvious way: through a byte array copied into a fresh bitmap. */
internal fun argbFrameImage(width: Int, height: Int, pixels: IntArray): Pair<Bitmap, Image> {
    // Little-endian ARGB ints are B, G, R, A bytes.
    val bytes = ByteBuffer.allocate(pixels.size * BYTES_PER_PIXEL).order(ByteOrder.LITTLE_ENDIAN).also { it.asIntBuffer().put(pixels) }.array()
    val bitmap = Bitmap()
    check(bitmap.installPixels(bgraInfo(width, height), bytes, width * BYTES_PER_PIXEL)) { "Could not fill a $width by $height frame" }
    bitmap.setImmutable()
    return bitmap to Image.makeFromBitmap(bitmap)
}
