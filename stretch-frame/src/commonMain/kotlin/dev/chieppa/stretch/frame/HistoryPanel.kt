package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.core.Command
import dev.chieppa.stretch.core.History
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.history_redo_step
import dev.chieppa.stretch.frame.generated.history_start
import org.jetbrains.compose.resources.stringResource

/**
 * The document's undo history as a list, for a drawer: the starting document, every step that can be undone and,
 * dimmed, the steps that can be redone. The current step is selected; clicking any row undoes or redoes to it, so
 * nothing is lost until a new change replaces the dimmed steps. [label] names a step, such as "Delete layer Tail".
 */
@Composable
fun <D> HistoryPanel(history: History<D>, label: (Command<D>) -> String, modifier: Modifier = Modifier) {
    val steps by history.steps.collectAsState()
    val current = steps.done.size
    val redoState = stringResource(Res.string.history_redo_step)
    Column(modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ListRow(selected = current == 0, onClick = { history.jumpTo(0) }) { RowText(stringResource(Res.string.history_start)) }
        steps.done.forEachIndexed { i, step ->
            ListRow(selected = current == i + 1, onClick = { history.jumpTo(i + 1) }) { RowText(label(step)) }
        }
        steps.undone.forEachIndexed { i, step ->
            ListRow(
                selected = false,
                onClick = { history.jumpTo(current + i + 1) },
                modifier = Modifier.semantics { stateDescription = redoState },
            ) { RowText(label(step), subtle = true) }
        }
    }
}
