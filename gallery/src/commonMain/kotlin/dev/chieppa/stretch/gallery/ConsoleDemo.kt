package dev.chieppa.stretch.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.console.Console
import dev.chieppa.stretch.console.ConsoleStream
import dev.chieppa.stretch.console.rememberConsoleState
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.Label
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ESC = "\u001B["

/** Pretend build output: colors, a progress bar drawn with carriage returns, errors with `path:line` links. */
@Composable
internal fun ConsoleDemo() {
    val state = rememberConsoleState()
    val scope = rememberCoroutineScope()
    var clicked by remember { mutableStateOf("Click a path:line link in the output.") }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button("Run a build", {
                scope.launch {
                    state.append("${ESC}1mBuilding${ESC}0m sample project\n")
                    for (step in 0..20) {
                        state.append("\r[${"#".repeat(step)}${" ".repeat(20 - step)}] ${step * 5}%")
                        delay(40)
                    }
                    state.append("\n")
                    for (n in 1..200) state.append("${ESC}2mcompiling${ESC}22m src/part$n/Shape.kt ${ESC}32mok${ESC}39m\n")
                    state.append("src/part7/Shape.kt:12:5: ${ESC}31mtype mismatch${ESC}39m\n", ConsoleStream.Err)
                    state.append("${ESC}33mwarning:${ESC}0m docs/guide.md:40 has a broken link\n")
                    state.append("${ESC}1;32mDone${ESC}0m in 1.2s\n")
                }
            })
            Button("Clear", { state.clear() })
            Label(clicked, Stretch.type.bodySm, Stretch.colors.inkMuted)
        }
        Console(state, Modifier.weight(1f).fillMaxWidth(), onLinkClick = { clicked = "Open ${it.target} at line ${it.line}" + (it.column?.let { c -> ", column $c" } ?: "") })
    }
}
