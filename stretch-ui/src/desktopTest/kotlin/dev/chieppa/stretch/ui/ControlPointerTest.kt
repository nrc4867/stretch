package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.pointerIconsAt
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals

class ControlPointerTest {
    @Test
    fun controlsShowTheArrowWhateverIsAroundThem() {
        val icons = pointerIconsAt(IntSize(400, 400), listOf(Offset(300f, 300f), Offset(10f, 10f), Offset(10f, 60f))) {
            StretchTheme(StretchThemes.Ink) {
                Box(Modifier.fillMaxSize().pointerHoverIcon(PointerIcon.Text)) {
                    Column {
                        Button("Reply", {}, Modifier.size(100.dp, 40.dp))
                        IconButton(StretchIcons.Close, "Close", {}, Modifier.size(40.dp))
                    }
                }
            }
        }
        assertEquals(listOf(PointerIcon.Text, PointerIcon.Default, PointerIcon.Default), icons)
    }
}
