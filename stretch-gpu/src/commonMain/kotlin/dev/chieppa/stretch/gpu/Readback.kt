package dev.chieppa.stretch.gpu

/** Copying a texture into a buffer needs each row to start on a multiple of this many bytes. */
const val COPY_ROW_ALIGNMENT = 256

private const val BYTES_PER_PIXEL = 4

/** The bytes per row of a [width] pixel, 4-byte-per-pixel image in a buffer that a texture is copied into. */
fun paddedRowBytes(width: Int): Int = (width * BYTES_PER_PIXEL + COPY_ROW_ALIGNMENT - 1) / COPY_ROW_ALIGNMENT * COPY_ROW_ALIGNMENT

/**
 * Packed ARGB pixels, top row first, from 8-bit-per-channel [bytes] laid out [rowBytes] apart. [bgra] says the bytes
 * are in blue, green, red, alpha order rather than red, green, blue, alpha.
 */
fun argbPixels(bytes: ByteArray, width: Int, height: Int, rowBytes: Int, bgra: Boolean): IntArray {
    val pixels = IntArray(width * height)
    val red = if (bgra) 2 else 0
    val blue = if (bgra) 0 else 2
    for (y in 0 until height) {
        for (x in 0 until width) {
            val i = y * rowBytes + x * BYTES_PER_PIXEL
            fun channel(offset: Int) = bytes[i + offset].toInt() and 0xFF
            pixels[y * width + x] = (channel(3) shl 24) or (channel(red) shl 16) or (channel(1) shl 8) or channel(blue)
        }
    }
    return pixels
}
