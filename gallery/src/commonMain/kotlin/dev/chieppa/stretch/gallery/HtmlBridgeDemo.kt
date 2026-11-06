package dev.chieppa.stretch.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.Label
import dev.chieppa.stretch.web.HtmlContent
import dev.chieppa.stretch.web.HtmlView
import dev.chieppa.stretch.web.rememberHtmlBridge

/** A page styled with the theme's custom properties, trading messages with the app through a bridge. */
private const val BRIDGE_PAGE = """<!doctype html>
<style>
  body { font: 14px system-ui, sans-serif; margin: 16px; }
  button { background: var(--stretch-surface-control); color: var(--stretch-ink-strong); border: var(--stretch-stroke) solid var(--stretch-line-control);
           border-radius: var(--stretch-radius-ctl); padding: 6px 12px; font: inherit; }
  .note { background: var(--stretch-surface-panel); border-radius: var(--stretch-radius-card); padding: 12px; margin-top: 12px; }
</style>
<p>This page uses the theme's tokens as CSS custom properties.</p>
<button id="send">Send to the app</button>
<div class="note" id="heard">Nothing from the app yet.</div>
<script>
  let count = 0;
  document.getElementById('send').onclick = () => stretch.post('Clicked in the page ' + (++count) + ' times');
  stretch.listen((m) => { document.getElementById('heard').textContent = 'The app says: ' + m; });
</script>"""

@Composable
internal fun HtmlBridgeDemo() {
    var heard by remember { mutableStateOf("Nothing from the page yet.") }
    var sent by remember { mutableIntStateOf(0) }
    val bridge = rememberHtmlBridge { heard = it }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button("Send to the page", { bridge.post("hello number ${++sent}") })
            Label(heard, Stretch.type.body, Stretch.colors.ink)
        }
        HtmlView(HtmlContent.Inline(BRIDGE_PAGE), Modifier.weight(1f).fillMaxWidth(), bridge = bridge)
    }
}
