package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.pointerIconsAt
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals

class CodePointerTest {
    private val name = "a".repeat(120)
    private val text = CodeText.of("$name\n${"b".repeat(120)}")
    private val slots = CodeViewerSlots(symbolAt = { if (it.line == 1) CodeRange(1, 0, name.length) else null }, onSymbolClick = {})
    private val overFirst = Offset(400f, 8f)
    private val overSecond = Offset(400f, 26f)

    private fun iconsAt(keys: PointerKeyboardModifiers) = pointerIconsAt(IntSize(700, 300), listOf(overFirst, overSecond), keys) {
        StretchTheme(StretchThemes.Ink) { CodeViewer(text, Modifier.size(700.dp, 300.dp), slots = slots) }
    }

    @Test
    fun aSymbolUnderAHeldCtrlShowsAHand() {
        assertEquals(listOf(PointerIcon.Hand, PointerIcon.Text), iconsAt(PointerKeyboardModifiers(isCtrlPressed = true)))
    }

    @Test
    fun withoutCtrlTheCodeShowsATextBeam() {
        assertEquals(listOf(PointerIcon.Text, PointerIcon.Text), iconsAt(PointerKeyboardModifiers()))
    }
}
