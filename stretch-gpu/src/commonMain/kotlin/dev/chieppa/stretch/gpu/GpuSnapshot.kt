package dev.chieppa.stretch.gpu

/**
 * Renders a [GpuScene] once into an image instead of a window: for exporting a still, and for checking what a scene draws without
 * opening one. Sets up a device of its own, so it is slow to start and meant for single images; [GpuSnapshots] keeps one for a run of
 * images. On the desktop it runs on the calling thread.
 */
object GpuSnapshot {
    suspend fun <S> render(
        state: S,
        width: Int,
        height: Int,
        scale: Float = 1f,
        options: GpuOptions = GpuOptions(),
        scene: (GpuContext) -> GpuScene<S>,
    ): GpuImage {
        require(width > 0 && height > 0) { "An image needs a size" }
        return GpuSnapshots.open(options, scene).use { it.render(state, width, height, scale) }
    }
}
