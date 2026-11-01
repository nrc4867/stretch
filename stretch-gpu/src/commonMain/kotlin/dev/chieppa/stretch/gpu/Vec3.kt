package dev.chieppa.stretch.gpu

import androidx.compose.runtime.Immutable
import kotlin.math.sqrt

/** A point or direction in a 3D scene. */
@Immutable
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)

    operator fun times(scale: Float) = Vec3(x * scale, y * scale, z * scale)

    operator fun unaryMinus() = Vec3(-x, -y, -z)

    infix fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    infix fun cross(other: Vec3) = Vec3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x)

    val length: Float get() = sqrt(this dot this)

    /** This direction at length 1; zero stays zero. */
    fun normalized(): Vec3 = length.let { if (it == 0f) this else this * (1f / it) }

    companion object {
        val Zero = Vec3(0f, 0f, 0f)
        val Up = Vec3(0f, 1f, 0f)
    }
}
