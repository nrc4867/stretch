package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSpacing
import dev.chieppa.stretch.ui.Label

/** The least room a subtitle shows in; with less, only the title shows. */
private val SubtitleLeastRoom = StretchSpacing.space24

/**
 * A tab's [title] in [ink] and, after it on the same baseline, its quieter [subtitle] in [subtitleInk]. The title
 * keeps the room it needs and the subtitle takes what is left, so the subtitle is cut short first; the title is cut
 * short only once the subtitle is gone. Screen readers hear both, shown or not. [onCut] hears whether any text was
 * cut short.
 */
@Composable
internal fun TabTitle(
    title: String,
    subtitle: String?,
    ink: Color,
    subtitleInk: Color,
    modifier: Modifier = Modifier,
    onCut: (Boolean) -> Unit = {},
) {
    val cut by rememberUpdatedState(onCut)
    Layout(
        content = {
            Label(title, Stretch.type.control, ink)
            if (subtitle != null) Label(subtitle, Stretch.type.bodySm, subtitleInk)
        },
        // Both texts stay in the semantics, the subtitle too when there is no room to show it.
        modifier = modifier.clearAndSetSemantics { this[SemanticsProperties.Text] = listOfNotNull(title, subtitle).map(::AnnotatedString) },
    ) { measurables, constraints ->
        val gap = StretchSpacing.space6.roundToPx()
        val least = SubtitleLeastRoom.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val titleText = measurables[0]
        val first = titleText.measure(loose)
        val subtitleText = measurables.getOrNull(1)
        val room = if (constraints.hasBoundedWidth) constraints.maxWidth - first.width - gap else Constraints.Infinity
        val second = if (subtitleText != null && room >= least) subtitleText.measure(loose.copy(maxWidth = room)) else null
        if (constraints.hasBoundedWidth) {
            val height = constraints.maxHeight.takeIf { constraints.hasBoundedHeight } ?: Constraints.Infinity
            val wanted = titleText.maxIntrinsicWidth(height) + (subtitleText?.let { gap + it.maxIntrinsicWidth(height) } ?: 0)
            cut(wanted > first.width + (second?.let { gap + it.width } ?: 0))
        }
        val width = first.width + (second?.let { gap + it.width } ?: 0)
        val height = maxOf(first.height, second?.height ?: 0)
        layout(constraints.constrainWidth(width), constraints.constrainHeight(height)) {
            val titleTop = (height - first.height) / 2
            first.placeRelative(0, titleTop)
            if (second != null) {
                val a = first[FirstBaseline]
                val b = second[FirstBaseline]
                val top = if (a == AlignmentLine.Unspecified || b == AlignmentLine.Unspecified) (height - second.height) / 2 else titleTop + a - b
                second.placeRelative(first.width + gap, top)
            }
        }
    }
}
