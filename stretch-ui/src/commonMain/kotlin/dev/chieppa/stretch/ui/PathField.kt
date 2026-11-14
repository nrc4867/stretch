package dev.chieppa.stretch.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.chieppa.stretch.theme.StretchIcons
import kotlinx.coroutines.launch

/**
 * A [TextField] for a path, in the value face, whose leading [icon] is a browse button: clicking it runs [onBrowse],
 * typically the platform's folder or file dialog, and a path it answers replaces the field's text through
 * [onValueChange]; null leaves the text as it was. [browseLabel] is the button's tooltip and its name for screen
 * readers. Without [onBrowse], where the platform has no dialog to offer, the icon is only a marker.
 */
@Composable
fun PathField(
    value: String,
    onValueChange: (String) -> Unit,
    browseLabel: String,
    onBrowse: (suspend () -> String?)?,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    contentDescription: String = placeholder,
    icon: ImageVector = StretchIcons.Folder,
    error: String? = null,
) {
    val scope = rememberCoroutineScope()
    val browse by rememberUpdatedState(onBrowse)
    val change by rememberUpdatedState(onValueChange)
    TextField(
        value,
        onValueChange,
        modifier,
        placeholder = placeholder,
        contentDescription = contentDescription,
        leading = {
            if (onBrowse == null) {
                FieldIcon(icon)
            } else {
                Tooltip(browseLabel) {
                    FieldIcon(icon, contentDescription = browseLabel, onClick = {
                        val ask = browse ?: return@FieldIcon
                        scope.launch { ask()?.let(change) }
                    })
                }
            }
        },
        error = error,
        mono = true,
    )
}
