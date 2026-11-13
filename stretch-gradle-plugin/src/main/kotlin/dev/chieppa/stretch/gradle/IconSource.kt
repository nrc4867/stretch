package dev.chieppa.stretch.gradle

import com.github.weisj.jsvg.SVGDocument
import com.github.weisj.jsvg.parser.LoaderContext
import com.github.weisj.jsvg.parser.SVGLoader
import com.github.weisj.jsvg.view.ViewBox
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** An app icon drawn at any pixel size: a PNG scaled down, or an SVG drawn at that size. */
fun interface IconSource {
    fun at(size: Int): BufferedImage

    companion object {
        private const val SVG = ".svg"
        private const val MIN_PNG = 256

        /** A square [image], at least 256px, scaled to each size. */
        fun of(image: BufferedImage): IconSource {
            require(image.width == image.height) { "App icon must be square, is ${image.width}x${image.height}" }
            require(image.width >= MIN_PNG) { "App icon must be at least ${MIN_PNG}px, is ${image.width}px; 1024px is best" }
            return IconSource { size -> AppIcons.scaled(image, size) }
        }

        /**
         * The icon in [file]: a PNG, or an SVG drawn at each size from itself, or from a hand-tuned `<name>-<size>.svg`
         * beside it where there is one for that size, such as `app-16.svg`.
         */
        fun read(file: File): IconSource {
            if (!file.name.endsWith(SVG, ignoreCase = true)) {
                return of(requireNotNull(ImageIO.read(file)) { "${file.name} is not a PNG the JDK can read" })
            }
            val main = svg(file)
            return IconSource { size -> draw(variant(file, size)?.let(::svg) ?: main, size) }
        }

        /** The hand-tuned SVG for [size] beside [file], when there is one. */
        fun variant(file: File, size: Int): File? = File(file.parentFile, "${file.nameWithoutExtension}-$size$SVG").takeIf { it.isFile }

        private fun svg(file: File): SVGDocument =
            requireNotNull(SVGLoader().load(file.toURI().toURL(), LoaderContext.createDefault())) { "${file.name} is not an SVG JSVG can read" }

        private fun draw(document: SVGDocument, size: Int): BufferedImage {
            val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
            val g = image.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            document.render(null, g, ViewBox(0f, 0f, size.toFloat(), size.toFloat()))
            g.dispose()
            return image
        }
    }
}
