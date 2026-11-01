package dev.chieppa.stretch.gpu

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

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
