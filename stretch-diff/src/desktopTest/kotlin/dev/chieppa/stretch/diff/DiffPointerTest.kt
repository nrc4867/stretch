package dev.chieppa.stretch.diff

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.pointerIconsAt
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals

class DiffPointerTest {
    private val added = diffTexts("", (1..200).joinToString("\n") { "row $it of a file that was added whole" }, null, "added.txt")

    @Test
    fun theGutterShowsAHandAndTheCodeATextBeam() {
        val slots = DiffSlots(onLineClick = {}, gutterMark = { BasicText("•") })
        val icons = pointerIconsAt(IntSize(700, 600), listOf(Offset(8f, 300f), Offset(300f, 300f))) {
            StretchTheme(StretchThemes.Ink) { DiffViewer(listOf(added), Modifier.size(700.dp, 600.dp), slots = slots) }
        }
        assertEquals(listOf(PointerIcon.Hand, PointerIcon.Text), icons)
    }
}
