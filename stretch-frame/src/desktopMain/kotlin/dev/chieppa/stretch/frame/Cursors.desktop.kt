package dev.chieppa.stretch.frame

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor

internal actual val resizeSideways: PointerIcon = PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))

internal actual val resizeUpDown: PointerIcon = PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR))

internal actual val moveAround: PointerIcon = PointerIcon(Cursor(Cursor.MOVE_CURSOR))

internal actual val resizeDiagonalDown: PointerIcon = PointerIcon(Cursor(Cursor.NW_RESIZE_CURSOR))

internal actual val resizeDiagonalUp: PointerIcon = PointerIcon(Cursor(Cursor.NE_RESIZE_CURSOR))
