package dev.chieppa.stretch.gpu

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

/**
 * A 3D camera at one moment, safe to hand to a render thread: where it is, what it looks at, and its lens. The
 * matrices need the surface's aspect ratio, so they are built where the size is known.
 */
@Immutable
data class CameraView(
    val eye: Vec3,
    val target: Vec3,
    val up: Vec3 = Vec3.Up,
    val fovY: Float = DEFAULT_FOV,
    val near: Float = 0.05f,
    val far: Float = 500f,
) {
    fun view(): Mat4 = Mat4.lookAt(eye, target, up)

    fun projection(aspect: Float): Mat4 = Mat4.perspective(fovY, aspect, near, far)

    fun viewProjection(width: Int, height: Int): Mat4 = projection(width.toFloat() / height.coerceAtLeast(1)) * view()

    /**
     * Where [point] appears on a [width] by [height] pixel surface, from its top-left corner; null when it is behind
     * the camera. For drawing overlays such as gizmos and handles at scene positions.
     */
    fun project(point: Vec3, width: Int, height: Int): Offset? {
        val clip = viewProjection(width, height).transform(point)
        val w = clip[3]
        if (w <= 0f) return null
        return Offset((clip[0] / w + 1f) / 2f * width, (1f - clip[1] / w) / 2f * height)
    }

    /**
     * The ray from the eye through [at] on a [width] by [height] pixel surface, measured from its top-left corner: what
     * is under that pixel lies along it. Its direction has length 1. The opposite of [project].
     */
    fun ray(at: Offset, width: Int, height: Int): Ray {
        val inverse = checkNotNull(viewProjection(width, height).inverse()) { "The camera's eye and target are the same point" }
        val x = at.x / width * 2f - 1f
        val y = 1f - at.y / height * 2f
        val near = checkNotNull(inverse.project(Vec3(x, y, 0f)))
        val far = checkNotNull(inverse.project(Vec3(x, y, 1f)))
        return Ray(eye, (far - near).normalized())
    }

    /**
     * The world point at [at] on a [width] by [height] pixel surface whose depth buffer holds [depth] there, from 0 at
     * the near plane to 1 at the far one, as GPU picking reads it back. Null when the camera cannot be undone.
     */
    fun unproject(at: Offset, depth: Float, width: Int, height: Int): Vec3? {
        val inverse = viewProjection(width, height).inverse() ?: return null
        return inverse.project(Vec3(at.x / width * 2f - 1f, 1f - at.y / height * 2f, depth))
    }

    companion object {
        /** 40 degrees, a natural lens for modelling. */
        const val DEFAULT_FOV = 0.698f
    }
}
