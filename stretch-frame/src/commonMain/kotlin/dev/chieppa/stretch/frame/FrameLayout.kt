package dev.chieppa.stretch.frame

enum class Side { Left, Right }

/** Where one drawer sits: its side, its order on that side, its width in dp and whether it is open. */
data class DrawerPlacement(val id: String, val side: Side, val width: Float = FrameLayout.DEFAULT_WIDTH, val open: Boolean = false)

/**
 * The drawer layout of one mode. Drawers dock left or right in list order; open drawers on the same side stack.
 * Every change returns a new layout, so the app keeps it as state and saves it per mode.
 */
data class FrameLayout(val drawers: List<DrawerPlacement>) {
    init {
        require(drawers.map { it.id }.toSet().size == drawers.size) { "Drawer ids must be unique" }
    }

    fun on(side: Side): List<DrawerPlacement> = drawers.filter { it.side == side }

    fun openOn(side: Side): List<DrawerPlacement> = on(side).filter { it.open }

    operator fun get(id: String): DrawerPlacement? = drawers.firstOrNull { it.id == id }

    fun toggle(id: String): FrameLayout = update(id) { it.copy(open = !it.open) }

    fun open(id: String): FrameLayout = update(id) { it.copy(open = true) }

    fun close(id: String): FrameLayout = update(id) { it.copy(open = false) }

    fun resize(id: String, width: Float): FrameLayout = update(id) { it.copy(width = width.coerceIn(MIN_WIDTH, MAX_WIDTH)) }

    /** Moves the drawer to the end of [side], keeping it open or closed. */
    fun dock(id: String, side: Side): FrameLayout {
        val drawer = get(id) ?: return this
        return FrameLayout(drawers.filter { it.id != id } + drawer.copy(side = side))
    }

    private fun update(id: String, change: (DrawerPlacement) -> DrawerPlacement): FrameLayout =
        FrameLayout(drawers.map { if (it.id == id) change(it) else it })

    companion object {
        const val MIN_WIDTH = 260f
        const val MAX_WIDTH = 420f
        const val DEFAULT_WIDTH = 300f

        /** Below this window width drawers float over the viewport instead of pushing it over. */
        const val NARROW_WINDOW = 1100f
    }
}

/** Each mode's layout, starting from the mode's default and remembered until reset. */
data class ModeLayouts(val defaults: Map<String, FrameLayout>, val current: Map<String, FrameLayout> = defaults) {
    operator fun get(mode: String): FrameLayout = current[mode] ?: defaults.getValue(mode)

    fun update(mode: String, change: (FrameLayout) -> FrameLayout): ModeLayouts =
        copy(current = current + (mode to change(get(mode))))

    fun reset(mode: String): ModeLayouts = copy(current = current + (mode to defaults.getValue(mode)))
}
