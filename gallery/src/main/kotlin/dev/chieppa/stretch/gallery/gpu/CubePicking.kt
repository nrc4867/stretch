package dev.chieppa.stretch.gallery.gpu

import dev.chieppa.stretch.gpu.Mat4
import dev.chieppa.stretch.gpu.Ray
import dev.chieppa.stretch.gpu.Vec3

/**
 * Where [ray] first meets the cube turned [angle] radians about the vertical axis, or null when it misses: the ray is
 * turned back into the cube's own space, where the cube is a box, and the hit turned forward again.
 */
fun rayPickCube(ray: Ray, angle: Float): Vec3? {
    val undo = Mat4.rotation(Vec3.Up, -angle)
    val local = Ray(undo.turn(ray.origin), undo.turn(ray.direction))
    val corner = Vec3(CUBE_HALF_SIZE, CUBE_HALF_SIZE, CUBE_HALF_SIZE)
    // Turning keeps lengths, so the distance along the local ray is the distance along the world one.
    val t = local.intersectBox(-corner, corner) ?: return null
    return ray.at(t)
}

/** [v] through this rotation; a rotation has no translation, so points and directions turn alike. */
private fun Mat4.turn(v: Vec3): Vec3 = transform(v).let { Vec3(it[0], it[1], it[2]) }
