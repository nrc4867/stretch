package dev.chieppa.stretch.ui

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/** Compact display for large values: 1,284 · 12.9K · 4.2M. */
fun compactNumber(value: Double): String {
    val sign = if (value < 0) "-" else ""
    val v = abs(value)
    fun one(x: Double): String {
        val r = (x * 10).roundToInt() / 10.0
        return if (r == floor(r)) r.toLong().toString() else r.toString()
    }
    return sign + when {
        v >= 1e9 -> one(v / 1e9) + "B"
        v >= 1e6 -> one(v / 1e6) + "M"
        v >= 1e4 -> one(v / 1e3) + "K"
        v == floor(v) -> groupThousands(v.toLong())
        else -> formatNumber(v.toFloat())
    }
}

/** 1284 as "1,284". */
fun groupThousands(value: Long): String =
    value.toString().reversed().chunked(3).joinToString(",").reversed()

/** Clean axis ticks from zero (or the data's minimum) up past [max]: steps of 1, 2 or 5 times a power of ten. */
fun niceTicks(min: Float, max: Float, target: Int = 4): List<Float> {
    val low = minOf(0f, min)
    val span = (max - low).takeIf { it > 0f } ?: 1f
    val raw = span / target
    val magnitude = 10.0.pow(floor(log10(raw.toDouble())))
    val step = listOf(1.0, 2.0, 5.0, 10.0).map { it * magnitude }.first { it >= raw }
    val start = floor(low / step) * step
    val end = ceil(max / step) * step
    val count = ((end - start) / step).roundToInt()
    return (0..count).map { (start + it * step).toFloat() }
}

/** Where a dragged row lands: its start slot moved by the drag offset, rounded to the nearest slot. */
fun dropIndex(from: Int, offset: Float, slot: Float, count: Int): Int =
    (from + (offset / slot).roundToInt()).coerceIn(0, count - 1)

/** The grid cell nearest to a point, for [columns] cells of [cell] size per row. */
fun gridIndexAt(x: Float, y: Float, cell: Float, columns: Int, count: Int): Int {
    val column = floor(x / cell).toInt().coerceIn(0, columns - 1)
    val row = floor(y / cell).toInt().coerceAtLeast(0)
    return (row * columns + column).coerceIn(0, count - 1)
}

/** [items] with the item at [from] moved to [to]. */
fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from == to) return this
    val result = toMutableList()
    result.add(to, result.removeAt(from))
    return result
}
