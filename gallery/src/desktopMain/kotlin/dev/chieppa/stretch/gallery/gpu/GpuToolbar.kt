package dev.chieppa.stretch.gallery.gpu

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.Toolbar
import dev.chieppa.stretch.frame.ToolbarSeparator
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.ToolButton

/** What a press on the 3D page's viewport does: select with a marquee and the frame's handles, or pick the cube. */
enum class GpuTool { Select, Pick }

/** The tool buttons' size inside the toolbar. */
private val ToolSize = 34.dp

/**
 * The 3D page's floating toolbar. Folding it hides the tools, so the corner it sits in changes size while it shows
 * over the native surface. Labels here are examples, not app text.
 */
@Composable
fun GpuToolbar(tool: GpuTool, onTool: (GpuTool) -> Unit, folded: Boolean, onFold: (Boolean) -> Unit) {
    Toolbar {
        if (!folded) {
            ToolButton(StretchIcons.Move, "Select", tool == GpuTool.Select, { onTool(GpuTool.Select) }, size = ToolSize)
            ToolButton(StretchIcons.Eyedropper, "Pick", tool == GpuTool.Pick, { onTool(GpuTool.Pick) }, size = ToolSize)
            ToolbarSeparator()
        }
        ToolButton(
            if (folded) StretchIcons.ChevronRight else StretchIcons.ChevronDown,
            if (folded) "Show tools" else "Fold tools",
            selected = false,
            onClick = { onFold(!folded) },
            size = ToolSize,
        )
    }
}
