package dev.chieppa.stretch.code

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/** Where an added or removed line's wash goes: behind the whole line, or behind its gutter only, the code left plain. */
enum class ChangeWash { Line, Gutter }

/** Whether a line in [tone] is washed on its gutter only. */
fun CodeLook.washesGutter(tone: LineTone?): Boolean =
    changeWash == ChangeWash.Gutter && (tone == LineTone.Added || tone == LineTone.Removed)

/** [color] behind the row: the whole of it, or the first [gutter] of it, as tall as the row, when given. */
fun Modifier.lineWash(color: Color, gutter: Dp? = null): Modifier =
    if (gutter == null) background(color) else drawBehind { drawRect(color, size = Size(gutter.toPx(), size.height)) }
