package dev.chieppa.stretch.gpu

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Determinants smaller than this count as zero: the matrix flattens space and has no inverse. */
private const val SINGULAR = 1e-12f

/**
 * A 4x4 matrix in column-major order, the layout WGSL's `mat4x4<f32>` reads from a buffer. Projections follow WebGPU's
 * conventions: right-handed view space looking down -z, and clip-space depth from 0 (near) to 1 (far).
 */
class Mat4 private constructor(private val m: FloatArray) {
    operator fun get(column: Int, row: Int): Float = m[column * 4 + row]

    operator fun times(other: Mat4): Mat4 {
        val out = FloatArray(16)
        for (column in 0..3) {
            for (row in 0..3) {
                var sum = 0f
                for (k in 0..3) sum += this[k, row] * other[column, k]
                out[column * 4 + row] = sum
            }
        }
        return Mat4(out)
    }

    /** [point] with w = 1 through this matrix, without the divide by w; returns x, y, z and w. */
    fun transform(point: Vec3): FloatArray = FloatArray(4) { row ->
        this[0, row] * point.x + this[1, row] * point.y + this[2, row] * point.z + this[3, row]
    }

    /**
     * The matrix that undoes this one, or null when there is none (its determinant is zero or close to it). Takes a
     * point from clip space back to the world when this is a view-projection.
     */
    fun inverse(): Mat4? {
        // Cofactor expansion over pairs of rows: s holds the 2x2 minors of the top two rows, c those of the bottom two.
        val a = m
        val s0 = a[0] * a[5] - a[4] * a[1]
        val s1 = a[0] * a[9] - a[8] * a[1]
        val s2 = a[0] * a[13] - a[12] * a[1]
        val s3 = a[4] * a[9] - a[8] * a[5]
        val s4 = a[4] * a[13] - a[12] * a[5]
        val s5 = a[8] * a[13] - a[12] * a[9]
        val c5 = a[10] * a[15] - a[14] * a[11]
        val c4 = a[6] * a[15] - a[14] * a[7]
        val c3 = a[6] * a[11] - a[10] * a[7]
        val c2 = a[2] * a[15] - a[14] * a[3]
        val c1 = a[2] * a[11] - a[10] * a[3]
        val c0 = a[2] * a[7] - a[6] * a[3]
        val det = s0 * c5 - s1 * c4 + s2 * c3 + s3 * c2 - s4 * c1 + s5 * c0
        if (abs(det) < SINGULAR) return null
        val d = 1f / det
        return of(
            (a[5] * c5 - a[9] * c4 + a[13] * c3) * d,
            (-a[1] * c5 + a[9] * c2 - a[13] * c1) * d,
            (a[1] * c4 - a[5] * c2 + a[13] * c0) * d,
            (-a[1] * c3 + a[5] * c1 - a[9] * c0) * d,
            (-a[4] * c5 + a[8] * c4 - a[12] * c3) * d,
            (a[0] * c5 - a[8] * c2 + a[12] * c1) * d,
            (-a[0] * c4 + a[4] * c2 - a[12] * c0) * d,
            (a[0] * c3 - a[4] * c1 + a[8] * c0) * d,
            (a[7] * s5 - a[11] * s4 + a[15] * s3) * d,
            (-a[3] * s5 + a[11] * s2 - a[15] * s1) * d,
            (a[3] * s4 - a[7] * s2 + a[15] * s0) * d,
            (-a[3] * s3 + a[7] * s1 - a[11] * s0) * d,
            (-a[6] * s5 + a[10] * s4 - a[14] * s3) * d,
            (a[2] * s5 - a[10] * s2 + a[14] * s1) * d,
            (-a[2] * s4 + a[6] * s2 - a[14] * s0) * d,
            (a[2] * s3 - a[6] * s1 + a[10] * s0) * d,
        )
    }

    /** [point] with w = 1 through this matrix, divided by w; null when w is zero, as for a point at infinity. */
    fun project(point: Vec3): Vec3? {
        val (x, y, z, w) = transform(point)
        return if (w == 0f) null else Vec3(x / w, y / w, z / w)
    }

    /** Copies the 16 values into [target] at [offset], ready for a uniform buffer. */
    fun copyInto(target: FloatArray, offset: Int = 0) = m.copyInto(target, offset)

    fun toFloatArray(): FloatArray = m.copyOf()

    override fun equals(other: Any?) = other is Mat4 && m.contentEquals(other.m)

    override fun hashCode() = m.contentHashCode()

    override fun toString() = "Mat4(${m.joinToString()})"

    companion object {
        val Identity = Mat4(FloatArray(16) { if (it % 5 == 0) 1f else 0f })

        /** A matrix from 16 column-major values. */
        fun of(vararg values: Float): Mat4 {
            require(values.size == 16) { "A Mat4 takes 16 values" }
            return Mat4(values.copyOf())
        }

        fun translation(offset: Vec3) = Mat4(FloatArray(16) { if (it % 5 == 0) 1f else 0f }.also {
            it[12] = offset.x
            it[13] = offset.y
            it[14] = offset.z
        })

        fun scale(factor: Float) = Mat4(FloatArray(16) { if (it % 5 == 0) factor else 0f }.also { it[15] = 1f })

        /** A rotation of [radians] around [axis], counter-clockwise looking down the axis. */
        fun rotation(axis: Vec3, radians: Float): Mat4 {
            val (x, y, z) = axis.normalized()
            val c = cos(radians)
            val s = sin(radians)
            val t = 1 - c
            return of(
                t * x * x + c, t * x * y + s * z, t * x * z - s * y, 0f,
                t * x * y - s * z, t * y * y + c, t * y * z + s * x, 0f,
                t * x * z + s * y, t * y * z - s * x, t * z * z + c, 0f,
                0f, 0f, 0f, 1f,
            )
        }

        /** A perspective projection with a vertical field of view of [fovY] radians. */
        fun perspective(fovY: Float, aspect: Float, near: Float, far: Float): Mat4 {
            val f = 1f / tan(fovY / 2)
            val range = 1f / (near - far)
            return of(
                f / aspect, 0f, 0f, 0f,
                0f, f, 0f, 0f,
                0f, 0f, far * range, -1f,
                0f, 0f, near * far * range, 0f,
            )
        }

        /** The view from [eye] towards [target], with [up] pointing up on screen. */
        fun lookAt(eye: Vec3, target: Vec3, up: Vec3 = Vec3.Up): Mat4 {
            val back = (eye - target).normalized()
            val right = (up cross back).normalized()
            val top = back cross right
            return of(
                right.x, top.x, back.x, 0f,
                right.y, top.y, back.y, 0f,
                right.z, top.z, back.z, 0f,
                -(right dot eye), -(top dot eye), -(back dot eye), 1f,
            )
        }
    }
}
