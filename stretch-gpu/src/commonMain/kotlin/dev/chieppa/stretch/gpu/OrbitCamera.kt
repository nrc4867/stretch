package dev.chieppa.stretch.gpu

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** How far the camera turns per pixel dragged, in radians. */
private const val ORBIT_PER_PIXEL = 0.008f

/** Keeps the camera just short of straight up or down, where the view's up direction flips. */
private const val PITCH_LIMIT = (PI / 2 - 0.01).toFloat()

/**
 * A camera that circles a [target]: [yaw] turns it around the vertical axis, [pitch] raises it above or below the
 * target, and [distance] is how far away it sits. Its values are Compose state, so a view that reads [view] redraws
 * when it moves. [Modifier.orbitNavigation] moves it with the mouse.
 */
@Stable
class OrbitCamera(
    target: Vec3 = Vec3.Zero,
    yaw: Float = 0.6f,
    pitch: Float = 0.35f,
    distance: Float = 6f,
    val distanceRange: ClosedFloatingPointRange<Float> = 0.5f..200f,
    val fovY: Float = CameraView.DEFAULT_FOV,
) {
    var target by mutableStateOf(target)
        private set

    var yaw by mutableFloatStateOf(yaw)
        private set

    var pitch by mutableFloatStateOf(pitch.coerceIn(-PITCH_LIMIT, PITCH_LIMIT))
        private set

    var distance by mutableFloatStateOf(distance.coerceIn(distanceRange))
        private set

    val eye: Vec3
        get() = target + Vec3(cos(pitch) * sin(yaw), sin(pitch), cos(pitch) * cos(yaw)) * distance

    /** The camera as it is now, to hand to a renderer. */
    val view: CameraView get() = CameraView(eye, target, fovY = fovY, near = distance * 0.01f, far = distance * 100f)

    /** Turns the camera around the target as for a drag of [dx], [dy] pixels. */
    fun orbitBy(dx: Float, dy: Float) {
        yaw -= dx * ORBIT_PER_PIXEL
        pitch = (pitch + dy * ORBIT_PER_PIXEL).coerceIn(-PITCH_LIMIT, PITCH_LIMIT)
    }

    /** Slides the camera and target sideways so the scene follows a drag of [dx], [dy] pixels on a [viewHeight] view. */
    fun panBy(dx: Float, dy: Float, viewHeight: Float) {
        if (viewHeight <= 0f) return
        val back = (eye - target).normalized()
        val right = (Vec3.Up cross back).normalized()
        val up = back cross right
        val unitsPerPixel = 2f * distance * tan(fovY / 2) / viewHeight
        target = target + right * (-dx * unitsPerPixel) + up * (dy * unitsPerPixel)
    }

    /** Moves the camera towards the target by [factor]: above 1 closer, below 1 further away. */
    fun dollyBy(factor: Float) {
        if (factor > 0f) distance = (distance / factor).coerceIn(distanceRange)
    }

    fun lookAt(target: Vec3, distance: Float = this.distance) {
        this.target = target
        this.distance = distance.coerceIn(distanceRange)
    }
}
