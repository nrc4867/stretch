package dev.chieppa.stretch.frame

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor

internal actual val resizeSideways: PointerIcon = PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))

internal actual val resizeUpDown: PointerIcon = PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR))

internal actual val moveAround: PointerIcon = PointerIcon(Cursor(Cursor.MOVE_CURSOR))
