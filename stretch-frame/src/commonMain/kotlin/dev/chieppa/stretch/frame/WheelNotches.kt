package dev.chieppa.stretch.frame

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange

/** How much one wheel notch adds to a pointer change's scroll delta on this platform. */
internal expect val wheelDeltaPerNotch: Float

/** How far this change scrolled, in wheel notches on every platform; a touchpad gives fractions. */
val PointerInputChange.wheelNotches: Offset get() = scrollDelta / wheelDeltaPerNotch
