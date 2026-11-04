package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.chieppa.stretch.ui.ButtonKind
import dev.chieppa.stretch.ui.SplitButtonFace

/**
 * A button with one main action and a chevron that opens a [Menu] of related ones: "Export" and its other formats.
 * Put [MenuItem]s in [menu] and call `close` from each.
 */
@Composable
fun SplitButton(
    text: String,
    onClick: () -> Unit,
    menuDescription: String,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Default,
    icon: ImageVector? = null,
    menu: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        SplitButtonFace(text, onClick, menuDescription, { open = true }, kind = kind, icon = icon, menuOpen = open)
        Menu(open, { open = false }) { menu { open = false } }
    }
}
