package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.tab_reload
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.theme.StretchSpacing
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.Tooltip
import org.jetbrains.compose.resources.stringResource

/**
 * A tab held back after its window crashed with it showing: the error mark on the error wash, the crash's [message]
 * in the error ink, selectable to copy, and [onReload].
 */
@Composable
internal fun HeldTab(message: String, onReload: () -> Unit) {
    val c = Stretch.colors
    Box(Modifier.fillMaxSize().background(c.errorWash).padding(StretchSpacing.space24), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(StretchSpacing.space12)) {
            Icon(StretchIcons.Error, tint = c.errorInk, size = StretchSizes.seg)
            if (message.isNotEmpty()) {
                SelectionContainer {
                    BasicText(
                        message,
                        style = Stretch.type.bodySm.merge(TextStyle(color = c.errorInk, textAlign = TextAlign.Center)),
                        maxLines = MESSAGE_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val reload = stringResource(Res.string.tab_reload)
            Tooltip(reload) { IconButton(StretchIcons.Rotate, reload, onReload, size = StretchSizes.seg) }
        }
    }
}

/** How many lines of a crash's message a held tab shows before cutting it short. */
private const val MESSAGE_LINES = 3
