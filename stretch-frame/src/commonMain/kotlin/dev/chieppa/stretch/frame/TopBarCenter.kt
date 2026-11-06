package dev.chieppa.stretch.frame

/** Where the top bar's centre item goes: its start and width, in the bar's content pixels from its start edge. */
internal data class CenterSpan(val x: Int, val width: Int)

/**
 * Where the top bar's centre item, [wanted] pixels wide, goes in content [width] pixels wide whose centre is [center]
 * pixels from its start: centred on [center], shifted to stay between [leftEnd] and [rightStart], then narrowed down
 * to [min] (or [wanted], if smaller). Null when it does not fit even then, or wants no room.
 */
internal fun centerSpan(width: Int, center: Int, leftEnd: Int, rightStart: Int, wanted: Int, min: Int): CenterSpan? {
    if (wanted <= 0) return null
    val room = minOf(rightStart, width) - maxOf(leftEnd, 0)
    val shown = minOf(wanted, room)
    if (shown < minOf(min, wanted)) return null
    val x = (center - shown / 2).coerceIn(maxOf(leftEnd, 0), minOf(rightStart, width) - shown)
    return CenterSpan(x, shown)
}
