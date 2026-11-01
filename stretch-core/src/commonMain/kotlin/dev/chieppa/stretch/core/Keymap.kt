package dev.chieppa.stretch.core

/** Default keys from the registry, with the person's changes on top. */
class Keymap(private val registry: ActionRegistry, changes: Map<ActionId, List<KeyChord>> = emptyMap()) {
    private val overrides = changes.toMutableMap()

    val changes: Map<ActionId, List<KeyChord>> get() = overrides

    fun keysFor(id: ActionId): List<KeyChord> = overrides[id] ?: registry[id]?.keys.orEmpty()

    fun rebind(id: ActionId, keys: List<KeyChord>) {
        overrides[id] = keys
    }

    fun reset(id: ActionId) {
        overrides.remove(id)
    }

    /** The available action bound to [chord] in [mode], preferring one scoped to the mode over a global one. */
    fun resolve(chord: KeyChord, mode: String?): Action? {
        val candidates = registry.available(mode).filter { chord in keysFor(it.id) }
        return candidates.firstOrNull { it.mode != null } ?: candidates.firstOrNull()
    }

    /** Chords bound to two actions that can be active at the same time. */
    fun conflicts(): List<Conflict> {
        val byChord = registry.all
            .flatMap { action -> keysFor(action.id).map { it to action } }
            .groupBy({ it.first }, { it.second })
        return byChord.flatMap { (chord, actions) ->
            actions.indices.flatMap { i ->
                (i + 1 until actions.size).mapNotNull { j ->
                    val a = actions[i]
                    val b = actions[j]
                    if (a.mode == null || b.mode == null || a.mode == b.mode) Conflict(chord, a.id, b.id) else null
                }
            }
        }
    }

    data class Conflict(val chord: KeyChord, val first: ActionId, val second: ActionId)
}
