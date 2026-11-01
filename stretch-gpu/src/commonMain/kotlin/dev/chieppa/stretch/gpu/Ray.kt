package dev.chieppa.stretch.gpu

import androidx.compose.runtime.Immutable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Below this, a ray counts as running parallel to a plane or a triangle, which it then never meets. */
private const val PARALLEL = 1e-7f

/**
 * A half-line from [origin] along [direction], for picking what is under a pointer (see [CameraView.ray]). The hit
 * tests return the distance along it as t, where [at] gives the point: in units of [direction]'s length, which is 1
 * for the rays a camera makes. Only hits at t >= 0, in front of the origin, count.
 */
@Immutable
data class Ray(val origin: Vec3, val direction: Vec3) {
    fun at(t: Float): Vec3 = origin + direction * t

    /** Where the ray meets the plane through [point] facing [normal], or null when it runs parallel or points away. */
    fun intersectPlane(point: Vec3, normal: Vec3): Vec3? {
        val facing = direction dot normal
        if (abs(facing) < PARALLEL) return null
        val t = ((point - origin) dot normal) / facing
        return if (t < 0f) null else at(t)
    }

    /** The nearest t where the ray meets the sphere at [center], or null when it misses; from inside, where it leaves. */
    fun intersectSphere(center: Vec3, radius: Float): Float? {
        val toOrigin = origin - center
        val a = direction dot direction
        val b = toOrigin dot direction
        val c = (toOrigin dot toOrigin) - radius * radius
        val discriminant = b * b - a * c
        if (discriminant < 0f || a == 0f) return null
        val root = sqrt(discriminant)
        val near = (-b - root) / a
        val far = (-b + root) / a
        return when {
            near >= 0f -> near
            far >= 0f -> far
            else -> null
        }
    }

    /** The t where the ray meets triangle [a], [b], [c] from either side, or null when it misses (Möller–Trumbore). */
    fun intersectTriangle(a: Vec3, b: Vec3, c: Vec3): Float? {
        val ab = b - a
        val ac = c - a
        val p = direction cross ac
        val det = ab dot p
        if (abs(det) < PARALLEL) return null
        val inverse = 1f / det
        val fromA = origin - a
        val u = (fromA dot p) * inverse
        if (u < 0f || u > 1f) return null
        val q = fromA cross ab
        val v = (direction dot q) * inverse
        if (v < 0f || u + v > 1f) return null
        val t = (ac dot q) * inverse
        return if (t < 0f) null else t
    }

    /**
     * The t where the ray enters the axis-aligned box from [min] to [max], or null when it misses; from inside, where
     * it leaves.
     */
    fun intersectBox(min: Vec3, max: Vec3): Float? {
        var enter = Float.NEGATIVE_INFINITY
        var leave = Float.POSITIVE_INFINITY
        for (axis in 0..2) {
            val o = origin[axis]
            val d = direction[axis]
            val low = min[axis]
            val high = max[axis]
            if (abs(d) < PARALLEL) {
                if (o < low || o > high) return null
                continue
            }
            val t1 = (low - o) / d
            val t2 = (high - o) / d
            enter = max(enter, min(t1, t2))
            leave = min(leave, max(t1, t2))
            if (enter > leave) return null
        }
        return when {
            enter >= 0f -> enter
            leave >= 0f -> leave
            else -> null
        }
    }
}

/** The x, y or z of this vector by index 0, 1 or 2. */
private operator fun Vec3.get(axis: Int): Float = when (axis) {
    0 -> x
    1 -> y
    else -> z
}
