package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.document_edited
import dev.chieppa.stretch.theme.Stretch
import org.jetbrains.compose.resources.stringResource

/** The document's name, and a dot after it while it has unsaved changes. */
@Composable
internal fun DocumentName(document: String, edited: Boolean) {
    val c = Stretch.colors
    val editedText = stringResource(Res.string.document_edited)
    Row(
        Modifier.semantics(mergeDescendants = true) { if (edited) stateDescription = editedText },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            document,
            Modifier.weight(1f, fill = false),
            style = Stretch.type.body.merge(TextStyle(color = c.inkSubtle)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (edited) Box(Modifier.size(7.dp).background(c.accent, CircleShape))
    }
}
