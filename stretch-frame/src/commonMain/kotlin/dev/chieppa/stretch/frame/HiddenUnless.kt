package dev.chieppa.stretch.frame

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics

/** Keeps an unplaced element out of screen readers and tests; unplaced nodes already get no focus. */
internal fun Modifier.hiddenUnless(visible: Boolean): Modifier = if (visible) this else clearAndSetSemantics {}
