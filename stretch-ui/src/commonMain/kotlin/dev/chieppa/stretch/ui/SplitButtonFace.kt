package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes

/**
 * The two halves of a split button: the main action and a chevron. stretch-frame's `SplitButton` adds the menu; use
 * that one unless your app shows its own panel from [onOpen].
 */
@Composable
fun SplitButtonFace(
    text: String,
    onClick: () -> Unit,
    openDescription: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Default,
    icon: ImageVector? = null,
    menuOpen: Boolean = false,
) {
    val radius = Stretch.shapes.ctl
    val main = rememberControlState()
    val open = rememberControlState()
    val outer = Stretch.shapes.ctlShape
    val mainShape = RoundedCornerShape(topStart = radius, bottomStart = radius)
    val openShape = RoundedCornerShape(topEnd = radius, bottomEnd = radius)
    val (mainFill, edge, ink) = buttonLook(kind, main)
    val (openFill) = buttonLook(kind, open)
    Row(modifier.height(StretchSizes.ctl).clip(outer).border(Stretch.stroke, edge, outer), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .fillMaxHeight()
                .focusRing(main, mainShape, mainFill)
                .background(mainFill)
                .hoverable(main.source)
                .controlPointer().clickable(main.source, null, role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(iconGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, ink)
            val style = Stretch.type.control
            ControlLabel(text, if (kind == ButtonKind.Primary) style.copy(fontWeight = FontWeight.SemiBold) else style, ink)
        }
        Box(Modifier.fillMaxHeight().width(Stretch.stroke).background(edge))
        Box(
            Modifier
                .opensOnPress(menuOpen, onOpen)
                .fillMaxHeight()
                .width(28.dp)
                .focusRing(open, openShape, openFill)
                .background(openFill)
                .hoverable(open.source)
                .controlPointer().clickable(open.source, null, role = Role.Button, onClick = onOpen)
                .semantics { contentDescription = openDescription },
            contentAlignment = Alignment.Center,
        ) {
            Icon(StretchIcons.ChevronDown, ink)
        }
    }
}
