package dev.chieppa.stretch.ui

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImageDecodingTest {
    private fun encoded(format: EncodedImageFormat): ByteArray {
        val surface = Surface.makeRasterN32Premul(30, 20)
        surface.canvas.clear(Color.RED)
        return surface.makeImageSnapshot().encodeToData(format)!!.bytes
    }

    @Test
    fun decodesRasterPictures() {
        for (format in listOf(EncodedImageFormat.PNG, EncodedImageFormat.JPEG, EncodedImageFormat.WEBP)) {
            val image = decodeImage(encoded(format))!!
            assertEquals(30 to 20, image.width to image.height, format.name)
        }
    }

    @Test
    fun drawsAnSvgAtTheSizeItGives() {
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" width="40" height="24"><rect width="40" height="24" fill="red"/></svg>"""
        val image = decodeImage(svg.encodeToByteArray())!!
        assertEquals(40 to 24, image.width to image.height)
    }

    @Test
    fun drawsAnSvgWithOnlyAViewBoxAtTheBoxsSize() {
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16"><circle cx="8" cy="8" r="6"/></svg>"""
        val image = decodeImage(svg.encodeToByteArray())!!
        assertEquals(16 to 16, image.width to image.height)
    }

    @Test
    fun otherBytesAreNoPicture() {
        assertNull(decodeImage(byteArrayOf()))
        assertNull(decodeImage(byteArrayOf(0, 1, 2, 3, 4, 5)))
        assertNull(decodeImage("plain text".encodeToByteArray()))
    }

    @Test
    fun aLargePictureShrinksToFitAndASmallOneKeepsItsSize() {
        assertEquals(0.5f, fitScale(DpSize(400.dp, 100.dp), DpSize(200.dp, 300.dp)))
        assertEquals(1f, fitScale(DpSize(40.dp, 10.dp), DpSize(200.dp, 300.dp)))
    }
}
