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

    companion object {
        /** 40 degrees, a natural lens for modelling. */
        const val DEFAULT_FOV = 0.698f
    }
}
