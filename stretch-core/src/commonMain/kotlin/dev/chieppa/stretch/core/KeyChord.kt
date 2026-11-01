package dev.chieppa.stretch.core

/** A key with modifiers, written the way people read it: "Ctrl Shift Z". */
data class KeyChord(
    val key: String,
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
    val meta: Boolean = false,
) {
    override fun toString(): String = buildList {
        if (ctrl) add("Ctrl")
        if (alt) add("Alt")
        if (shift) add("Shift")
        if (meta) add("Meta")
        add(key)
    }.joinToString(" ")

    companion object {
        private val modifierNames = setOf("ctrl", "shift", "alt", "meta")

        fun parse(text: String): KeyChord {
            val parts = text.trim().split(Regex("[\\s+]+")).filter { it.isNotEmpty() }
            require(parts.isNotEmpty()) { "Empty key chord" }
            val modifiers = parts.dropLast(1).map { it.lowercase() }
            modifiers.forEach { require(it in modifierNames) { "Unknown modifier '$it' in '$text'" } }
            val key = parts.last()
            return KeyChord(
                key = if (key.length == 1) key.uppercase() else key,
                ctrl = "ctrl" in modifiers,
                shift = "shift" in modifiers,
                alt = "alt" in modifiers,
                meta = "meta" in modifiers,
            )
        }
    }
}
