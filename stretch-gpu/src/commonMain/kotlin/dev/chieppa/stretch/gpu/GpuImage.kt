package dev.chieppa.stretch.gpu

/** An image read back from the GPU: [width] by [height] packed ARGB [pixels], top row first. */
class GpuImage(val width: Int, val height: Int, val pixels: IntArray) {
    operator fun get(x: Int, y: Int): Int = pixels[y * width + x]
}
