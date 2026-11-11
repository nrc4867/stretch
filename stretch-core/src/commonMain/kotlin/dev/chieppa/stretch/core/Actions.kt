package dev.chieppa.stretch.core

import kotlin.jvm.JvmInline

@JvmInline
value class ActionId(val value: String)

/**
 * Something a person can do, with one label, icon and set of default keys shared by menus, toolbars, tooltips and
 * the palette. [mode] limits the action and its keys to one mode; null means every mode.
 */
class Action(
    val id: ActionId,
    val textKey: String,
    val icon: String? = null,
    val keys: List<KeyChord> = emptyList(),
    val mode: String? = null,
    val isAvailable: () -> Boolean = { true },
    val run: () -> Unit,
)

class ActionRegistry {
    private val actions = linkedMapOf<ActionId, Action>()

    val all: Collection<Action> get() = actions.values

    fun register(action: Action) {
        require(action.id !in actions) { "Action ${action.id.value} is already registered" }
        actions[action.id] = action
    }

    /** Takes action [id] out, as when what contributed it goes away; its keys go with it. Gives the action, if it was registered. */
    fun unregister(id: ActionId): Action? = actions.remove(id)

    operator fun get(id: ActionId): Action? = actions[id]

    /** Actions that can run now in [mode]. An unavailable action is hidden, never shown disabled. */
    fun available(mode: String?): List<Action> =
        actions.values.filter { (it.mode == null || it.mode == mode) && it.isAvailable() }
}
