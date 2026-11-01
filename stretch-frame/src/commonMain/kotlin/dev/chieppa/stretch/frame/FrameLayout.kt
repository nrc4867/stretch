package dev.chieppa.stretch.frame

enum class Side { Left, Right }

/**
 * Where one drawer sits: its side, its order on that side, its width in dp and whether it is open. [bottom] puts its
 * rail button in the group at the foot of the rail, and its drawer below the others when both are open. [height]
 * is its share of the column when drawers stack.
 */
data class DrawerPlacement(
    val id: String,
    val side: Side,
    val width: Float = FrameLayout.DEFAULT_WIDTH,
    val open: Boolean = false,
    val bottom: Boolean = false,
    val height: Float = 1f,
)

/**
 * The drawer layout of one mode. Drawers dock left or right in list order; open drawers on the same side stack.
 * Every change returns a new layout, so the app keeps it as state and saves it per mode.
 */
data class FrameLayout(val drawers: List<DrawerPlacement>) {
    init {
        require(drawers.map { it.id }.toSet().size == drawers.size) { "Drawer ids must be unique" }
    }

    fun on(side: Side): List<DrawerPlacement> = drawers.filter { it.side == side }

    /** The rail group on [side]: the top group, or the bottom one. */
    fun group(side: Side, bottom: Boolean): List<DrawerPlacement> = on(side).filter { it.bottom == bottom }

    /** Open drawers on [side], top to bottom: the rail's top group first. */
    fun openOn(side: Side): List<DrawerPlacement> = on(side).filter { it.open }.sortedBy { it.bottom }

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

    /** Moves the drawer to [index] in the rail group given by [side] and [bottom], keeping it open or closed. */
    fun move(id: String, side: Side, bottom: Boolean, index: Int): FrameLayout {
        val drawer = get(id) ?: return this
        val rest = drawers.filter { it.id != id }
        val group = rest.filter { it.side == side && it.bottom == bottom }
        val at = index.coerceIn(0, group.size)
        val position = if (at < group.size) rest.indexOf(group[at]) else (rest.indexOfLast { it.side == side && it.bottom == bottom } + 1).takeIf { it > 0 } ?: rest.size
        return FrameLayout(rest.toMutableList().apply { add(position, drawer.copy(side = side, bottom = bottom)) })
    }

    /**
     * Moves the split between the open drawer [upper] and the one below it by [fraction] of the column's height.
     * Each keeps at least [MIN_SHARE] of the pair.
     */
    fun resizeSplit(upper: String, lower: String, fraction: Float): FrameLayout {
        val a = get(upper) ?: return this
        val b = get(lower) ?: return this
        val total = openOn(a.side).sumOf { it.height.toDouble() }.toFloat()
        val pair = a.height + b.height
        val newA = (a.height + fraction * total).coerceIn(pair * MIN_SHARE, pair * (1 - MIN_SHARE))
        return update(upper) { it.copy(height = newA) }.update(lower) { it.copy(height = pair - newA) }
    }

    private fun update(id: String, change: (DrawerPlacement) -> DrawerPlacement): FrameLayout =
        FrameLayout(drawers.map { if (it.id == id) change(it) else it })

    companion object {
        const val MIN_WIDTH = 260f
        const val MAX_WIDTH = 420f
        const val DEFAULT_WIDTH = 300f
        const val MIN_SHARE = 0.15f

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
