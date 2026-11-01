package dev.chieppa.stretch.gpu

import androidx.compose.runtime.Immutable

/**
 * What GPU picking found under a pixel: the [id] the scene drew there and the [depth] there, from 0 at the near plane
 * to 1 at the far one. [CameraView.unproject] turns the pixel and its depth into a point in the world.
 */
@Immutable
data class GpuPick(val id: Int, val depth: Float) {
    companion object {
        /** The ID of empty space: what the ID target is cleared to, and no object's ID. */
        const val NOTHING = 0

        /** Where a pick's texels land in its readback buffer: the ID first, its depth one copy row later. */
        const val ID_OFFSET = 0
        const val DEPTH_OFFSET = COPY_ROW_ALIGNMENT

        /** The readback buffer's size: one copy row for each texel. */
        const val BUFFER_BYTES = 2 * COPY_ROW_ALIGNMENT

        /** The pick in a readback buffer's [bytes], or null when it found [NOTHING]. */
        fun decode(bytes: ByteArray): GpuPick? {
            val id = littleEndianInt(bytes, ID_OFFSET)
            if (id == NOTHING) return null
            return GpuPick(id, Float.fromBits(littleEndianInt(bytes, DEPTH_OFFSET)))
        }
    }
}

private const val BYTE_MASK = 0xFF
private const val BITS_PER_BYTE = 8
private const val INT_BYTES = 4

/** The 32-bit integer stored least significant byte first at [offset] in [bytes], as GPUs lay out their texels. */
internal fun littleEndianInt(bytes: ByteArray, offset: Int): Int {
    var value = 0
    for (i in INT_BYTES - 1 downTo 0) value = (value shl BITS_PER_BYTE) or (bytes[offset + i].toInt() and BYTE_MASK)
    return value
}

/**
 * The clip-space matrix that blows pixel [x], [y] of a [width] by [height] pixel surface up to fill the whole view,
 * leaving depth alone: put in front of a view-projection, it draws just what covers that pixel into a 1 by 1 target.
 * Picking draws this way because a depth texture can only be copied whole.
 */
fun pickMatrix(x: Int, y: Int, width: Int, height: Int): Mat4 {
    // The pixel's centre in normalized device coordinates, which run -1..1 with y up.
    val cx = (x + PIXEL_CENTRE) / width * 2f - 1f
    val cy = 1f - (y + PIXEL_CENTRE) / height * 2f
    // One pixel spans 2 / width of that range; scaling by width stretches it over the whole range, 2.
    val sx = width.toFloat()
    val sy = height.toFloat()
    return Mat4.of(
        sx, 0f, 0f, 0f,
        0f, sy, 0f, 0f,
        0f, 0f, 1f, 0f,
        -sx * cx, -sy * cy, 0f, 1f,
    )
}

/** The middle of a pixel, from its top-left corner. */
private const val PIXEL_CENTRE = 0.5f
