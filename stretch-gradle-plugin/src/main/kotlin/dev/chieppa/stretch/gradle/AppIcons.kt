package dev.chieppa.stretch.gradle

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO

/** Writes every icon file an app needs from one square PNG: a Windows .ico, a macOS .icns and PNGs. */
internal object AppIcons {
    /** The sizes in a Windows .ico, which Explorer, the taskbar and Task Manager pick from. */
    val windowsSizes = listOf(16, 20, 24, 32, 40, 48, 64, 96, 128, 256)

    /** The sizes the running app hands its windows, so the OS can pick a sharp one at any scale. */
    val windowSizes = listOf(16, 20, 24, 32, 40, 48, 64, 128, 256)

    /** macOS icon types that hold a PNG, by pixel size. */
    private val macTypes = listOf("icp4" to 16, "icp5" to 32, "icp6" to 64, "ic07" to 128, "ic08" to 256, "ic09" to 512, "ic10" to 1024)

    fun read(file: File): BufferedImage {
        val image = requireNotNull(ImageIO.read(file)) { "${file.name} is not a PNG the JDK can read" }
        require(image.width == image.height) { "App icon ${file.name} must be square, is ${image.width}x${image.height}" }
        require(image.width >= 256) { "App icon ${file.name} must be at least 256px, is ${image.width}px; 1024px is best" }
        return image
    }

    /** Scales [source] to [size] in halving steps, which keeps small sizes sharp. */
    fun scaled(source: BufferedImage, size: Int): BufferedImage {
        var current = source
        while (current.width / 2 >= size) current = draw(current, current.width / 2)
        return if (current.width == size) current else draw(current, size)
    }

    private fun draw(source: BufferedImage, size: Int): BufferedImage {
        val out = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.drawImage(source, 0, 0, size, size, null)
        g.dispose()
        return out
    }

    fun png(image: BufferedImage): ByteArray = ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()

    /** An .ico whose entries are PNGs, which every Windows since Vista reads. */
    fun ico(source: BufferedImage): ByteArray {
        val entries = windowsSizes.filter { it <= source.width }.map { it to png(scaled(source, it)) }
        val header = ByteBuffer.allocate(6 + 16 * entries.size).order(ByteOrder.LITTLE_ENDIAN)
        header.putShort(0).putShort(1).putShort(entries.size.toShort())
        var offset = header.capacity()
        for ((size, data) in entries) {
            header.put((size % 256).toByte()).put((size % 256).toByte()).put(0).put(0)
            header.putShort(1).putShort(32).putInt(data.size).putInt(offset)
            offset += data.size
        }
        return ByteArrayOutputStream().apply {
            write(header.array())
            entries.forEach { write(it.second) }
        }.toByteArray()
    }

    /** An .icns whose entries are PNGs. */
    fun icns(source: BufferedImage): ByteArray {
        val entries = macTypes.filter { it.second <= source.width }.map { (type, size) -> type to png(scaled(source, size)) }
        val body = ByteArrayOutputStream()
        for ((type, data) in entries) {
            body.write(type.toByteArray(Charsets.US_ASCII))
            body.write(ByteBuffer.allocate(4).putInt(8 + data.size).array())
            body.write(data)
        }
        return ByteArrayOutputStream().apply {
            write("icns".toByteArray(Charsets.US_ASCII))
            write(ByteBuffer.allocate(4).putInt(8 + body.size()).array())
            write(body.toByteArray())
        }.toByteArray()
    }
}
