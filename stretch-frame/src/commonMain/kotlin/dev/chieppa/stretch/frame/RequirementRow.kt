package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.core.Requirement
import dev.chieppa.stretch.core.RequirementState
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.ButtonKind
import dev.chieppa.stretch.ui.ErrorMark
import dev.chieppa.stretch.ui.Label
import dev.chieppa.stretch.ui.ProgressBar

private const val BYTES_PER_MEGABYTE = 1_000_000L

/** Text for a [RequirementRow]. */
data class RequirementStrings(
    val get: (megabytes: Long?) -> String = { if (it == null) "Get" else "Get · $it MB" },
    val fetching: String = "Downloading",
    val cancel: String = "Cancel",
    val restart: String = "Restart to use it",
    val retry: String = "Try again",
)

/**
 * A [requirement] the app offers, under [name]: a button to get it, then its progress with a way to stop, then a
 * restart. It shows nothing once the requirement is ready. [onRestart] runs in place of [Requirement.restart], so the
 * app can save its work first; it should end by calling it.
 */
@Composable
fun RequirementRow(
    requirement: Requirement,
    name: String,
    modifier: Modifier = Modifier,
    onRestart: () -> Unit = requirement::restart,
    strings: RequirementStrings = RequirementStrings(),
) {
    val state by requirement.state.collectAsState()
    if (state == RequirementState.Ready) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Label(name, Stretch.type.control, Stretch.colors.inkStrong)
        when (val current = state) {
            RequirementState.Missing -> Button(strings.get(requirement.downloadBytes?.let { it / BYTES_PER_MEGABYTE }), requirement::install)
            is RequirementState.Installing -> {
                ProgressBar(current.fraction ?: 0f, strings.fetching, Modifier.width(160.dp))
                Button(strings.cancel, requirement::cancel, kind = ButtonKind.Quiet)
            }
            RequirementState.NeedsRestart -> Button(strings.restart, onRestart, kind = ButtonKind.Primary)
            is RequirementState.Failed -> {
                ErrorMark(current.reason)
                Button(strings.retry, requirement::install)
            }
            RequirementState.Ready -> Unit
        }
    }
}
