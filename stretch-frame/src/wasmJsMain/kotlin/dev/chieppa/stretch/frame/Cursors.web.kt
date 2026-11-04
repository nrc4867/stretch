@file:OptIn(ExperimentalComposeUiApi::class)

package dev.chieppa.stretch.frame

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.fromKeyword

internal actual val resizeSideways: PointerIcon = PointerIcon.fromKeyword("ew-resize")

internal actual val resizeUpDown: PointerIcon = PointerIcon.fromKeyword("ns-resize")

internal actual val moveAround: PointerIcon = PointerIcon.fromKeyword("move")

internal actual val resizeDiagonalDown: PointerIcon = PointerIcon.fromKeyword("nwse-resize")

internal actual val resizeDiagonalUp: PointerIcon = PointerIcon.fromKeyword("nesw-resize")
