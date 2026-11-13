package dev.chieppa.stretch.gradle

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class AppIconsTest {
    private val source = IconSource.of(
        BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB).apply {
            createGraphics().apply { color = Color(0xF5, 0xB8, 0x3D); fillRect(0, 0, 1024, 1024); dispose() }
        },
    )

    @Test
    fun icoHoldsEveryWindowsSizeAsReadablePngs() {
        val ico = ByteBuffer.wrap(AppIcons.ico(source)).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(1, ico.getShort(2).toInt(), "type: icon")
        val count = ico.getShort(4).toInt()
        assertEquals(AppIcons.windowsSizes.size, count)
        val sizes = (0 until count).map { i ->
            val entry = 6 + 16 * i
            val size = ico.getInt(entry + 8)
            val offset = ico.getInt(entry + 12)
            val png = ImageIO.read(ico.array().copyOfRange(offset, offset + size).inputStream())
            png.width
        }
        assertEquals(AppIcons.windowsSizes, sizes)
    }

    @Test
    fun icnsLengthsAddUp() {
        val icns = ByteBuffer.wrap(AppIcons.icns(source))
        assertEquals("icns", String(icns.array(), 0, 4))
        assertEquals(icns.capacity(), icns.getInt(4))
        var at = 8
        val types = mutableListOf<String>()
        while (at < icns.capacity()) {
            types += String(icns.array(), at, 4)
            at += icns.getInt(at + 4)
        }
        assertEquals(listOf("icp4", "icp5", "icp6", "ic07", "ic08", "ic09", "ic10"), types)
    }

    @Test
    fun anSvgIsDrawnAtEachSizeAndAHandTunedSizeWins() {
        val dir = createTempDirectory("stretch-icon").toFile()
        try {
            File(dir, "app.svg").writeText(square("#ff0000"))
            File(dir, "app-16.svg").writeText(square("#0000ff"))
            val icon = IconSource.read(File(dir, "app.svg"))
            val big = icon.at(1024)
            assertEquals(1024, big.width)
            assertEquals(0xffff0000.toInt(), big.getRGB(512, 512))
            assertEquals(0xff0000ff.toInt(), icon.at(16).getRGB(8, 8))
            assertEquals(0xffff0000.toInt(), icon.at(24).getRGB(12, 12))
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun square(color: String) = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 8 8"><rect width="8" height="8" fill="$color"/></svg>"""

    @Test
    fun packageVersionsAreNumeric() {
        assertEquals("0.1.0", packageVersion("0.1.0-SNAPSHOT"))
        assertEquals("2.3", packageVersion("2.3"))
        assertEquals("1.0.0", packageVersion("unspecified"))
    }

    @Test
    fun macPackageVersionsStartAboveZero() {
        assertEquals("2.3.1", macPackageVersion("2.3.1"))
        assertEquals("1.0.0", macPackageVersion("0.8.0"))
    }
}
