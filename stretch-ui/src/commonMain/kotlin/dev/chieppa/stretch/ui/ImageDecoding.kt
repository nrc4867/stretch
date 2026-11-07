package dev.chieppa.stretch.ui

import androidx.compose.ui.graphics.ImageBitmap

/**
 * A picture from a file's [bytes]: PNG, JPEG, GIF (its first frame), WebP, BMP, ICO or SVG, drawn at the size it
 * gives. Null when the bytes are none of these.
 */
expect fun decodeImage(bytes: ByteArray): ImageBitmap?
