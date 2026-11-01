package dev.chieppa.stretch.frame

import androidx.compose.ui.input.pointer.PointerIcon

/** The pointer over a handle that resizes sideways. */
internal expect val resizeSideways: PointerIcon

/** The pointer over a handle that resizes up and down. */
internal expect val resizeUpDown: PointerIcon

/** The pointer over something that moves when dragged, like a dialog header. */
internal expect val moveAround: PointerIcon

/** The pointer over a corner handle running from top-left to bottom-right. */
internal expect val resizeDiagonalDown: PointerIcon

/** The pointer over a corner handle running from bottom-left to top-right. */
internal expect val resizeDiagonalUp: PointerIcon
