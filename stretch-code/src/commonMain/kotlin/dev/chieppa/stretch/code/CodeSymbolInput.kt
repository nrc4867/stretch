package dev.chieppa.stretch.code

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** The keys that hold a code view's symbols ready to click through to. */
private val holdKeys = setOf(Key.CtrlLeft, Key.CtrlRight, Key.MetaLeft, Key.MetaRight)

/**
 * Notes Ctrl and Cmd going down and up while a code view has the keyboard, and lets go of them when it loses it.
 * Escape closes an open symbol card.
 */
fun Modifier.codeSymbolKeys(pointer: CodeSymbolPointer<*>): Modifier =
    onFocusChanged { if (!it.hasFocus) pointer.held = false }
        .onPreviewKeyEvent { event ->
            if (event.key in holdKeys) pointer.held = event.type == KeyEventType.KeyDown
            if (event.key == Key.Escape && event.type == KeyEventType.KeyDown && pointer.card.open != null) {
                pointer.card.close()
                true
            } else {
                false
            }
        }

/**
 * The line known by [keys] as [pointer] sees it: the first key is the one the pointer reports, and any others are
 * further names the same line answers to, such as an unchanged line's number on each side of a diff. Returns the
 * modifier for the line's text, which finds the character under the pointer through [geometry], takes a Ctrl or Cmd
 * press (so it never starts a selection) and clicks through on its release, notes where a plain press lands as the
 * caret, and shows a pointing hand over the underlined symbol. A plain press, click and drag select text as before, and
 * close a symbol card that is not pinned.
 */
@Composable
fun <G> rememberCodeSymbolInput(pointer: CodeSymbolPointer<G>, keys: List<CodeLineKey<G>>, geometry: CodeLineGeometry): Modifier {
    val primary = keys.first()
    DisposableEffect(pointer, keys, geometry) {
        pointer.track(keys, geometry)
        onDispose { pointer.untrack(keys, geometry) }
    }
    val hand by remember(pointer, primary) {
        derivedStateOf {
            val at = pointer.pointer
            val link = pointer.link
            at != null && link != null && CodeLineKey(at.group, at.line) == primary && at in link
        }
    }
    return Modifier
        .onGloballyPositioned { geometry.coordinates = it }
        .pointerHoverIcon(if (hand) PointerIcon.Hand else PointerIcon.Text, overrideDescendants = hand)
        .pointerInput(pointer, primary, geometry) {
            coroutineScope {
                val scope = this
                awaitPointerEventScope {
                    var taken = false
                    var pressed: CodeSpot<G>? = null
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue
                        pointer.held = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
                        fun spot(column: Int?) = column?.let { CodeSpot(primary.group, primary.line, it) }
                        val under = spot(geometry.columnUnder(change.position))
                        when (event.type) {
                            PointerEventType.Enter, PointerEventType.Move -> pointer.pointer = under
                            PointerEventType.Exit -> {
                                val at = pointer.pointer
                                if (at != null && at.group == primary.group && at.line == primary.line) pointer.pointer = null
                            }
                            PointerEventType.Press -> pointer.card.run {
                                pressing = true
                                dismiss()
                            }
                            PointerEventType.Release -> pointer.card.pressing = event.buttons.areAnyPressed
                        }
                        when (event.type) {
                            PointerEventType.Press -> if (event.buttons.isPrimaryPressed) {
                                if (pointer.enabled && pointer.held) {
                                    change.consume()
                                    taken = true
                                    pressed = under
                                } else {
                                    pointer.caret = spot(geometry.columnNearest(change.position))
                                }
                            }
                            PointerEventType.Release -> if (taken) {
                                change.consume()
                                taken = false
                                pressed?.let { spot -> scope.launch { pointer.click(spot) } }
                                pressed = null
                            }
                        }
                    }
                }
            }
        }
}
