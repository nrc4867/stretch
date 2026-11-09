package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.StretchPopup

/** How far below its field a suggestion panel sits, and how tall its results grow before they scroll. */
private val SUGGESTION_GAP = 4.dp
private val SUGGESTION_RESULTS_MAX = 420.dp

/**
 * Suggestions dropped under a search field while it is typed in, as a [SearchPalette]'s results are under its own field:
 * [content] made of [PaletteSection]s and [PaletteRow]s, scrolling past [maxHeight], and [hints] in a footer. Compose it
 * beside the field in one box, which it opens under at [width]. It never takes the keyboard, so the field keeps typing
 * and handles Up, Down and Enter itself; a click outside it calls [onDismiss].
 */
@Composable
fun SuggestionPanel(
    open: Boolean,
    onDismiss: () -> Unit,
    width: Dp,
    maxHeight: Dp = SUGGESTION_RESULTS_MAX,
    hints: List<PaletteHint> = emptyList(),
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!open) return
    val gap = with(LocalDensity.current) { SUGGESTION_GAP.roundToPx() }
    StretchPopup(BelowAnchor(gap), onDismiss, PopupProperties(focusable = false, dismissOnClickOutside = true), Stretch.shadows.float) {
        Column(Modifier.width(width).floating(Stretch.shapes.cardShape, Stretch.shadows.float)) {
            Column(Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState()), content = content)
            if (hints.isNotEmpty()) PaletteHints(hints)
        }
    }
}
