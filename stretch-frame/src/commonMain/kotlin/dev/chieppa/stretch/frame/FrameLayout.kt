package dev.chieppa.stretch.frame

enum class Side { Left, Right }

/** Where a drawer is showing, so its contents can lay out for a tall column or a wide strip. */
enum class DrawerPosition { Side, Bottom }

/**
 * Where one drawer sits: its side, its order on that side, its width in dp and whether it is open. [bottom] puts its
 * rail button in the group at the foot of the rail and opens it in the bottom panel instead of the side column.
 * [height] is its share of the side column when drawers stack.
 */
data class DrawerPlacement(
    val id: String,
    val side: Side,
    val width: Float = FrameLayout.DEFAULT_WIDTH,
    val open: Boolean = false,
    val bottom: Boolean = false,
    val height: Float = 1f,
) {
    val position: DrawerPosition get() = if (bottom) DrawerPosition.Bottom else DrawerPosition.Side
}

/**
 * The drawer layout of one mode. Drawers dock left or right in list order. Open drawers in a rail's top group stack
 * in that side's column; the bottom group opens one drawer at a time in the bottom panel, which spans the window
 * between the rails, or half of it when both sides have one open. Every change returns a new layout, so the app
 * keeps it as state and saves it per mode.
 */
data class FrameLayout(
    val drawers: List<DrawerPlacement>,
    val bottomHeight: Float = DEFAULT_BOTTOM_HEIGHT,
    val bottomSplit: Float = 0.5f,
) {
    init {
        require(drawers.map { it.id }.toSet().size == drawers.size) { "Drawer ids must be unique" }
        require(Side.entries.all { side -> drawers.count { it.side == side && it.bottom && it.open } <= 1 }) {
            "Only one bottom drawer per side can be open"
        }
    }

    fun on(side: Side): List<DrawerPlacement> = drawers.filter { it.side == side }

    /** The rail group on [side]: the top group, or the bottom one. */
    fun group(side: Side, bottom: Boolean): List<DrawerPlacement> = on(side).filter { it.bottom == bottom }

    /** Open drawers in [side]'s column, top to bottom. */
    fun openOn(side: Side): List<DrawerPlacement> = group(side, bottom = false).filter { it.open }

    /** The drawer open in [side]'s half of the bottom panel, if any. */
    fun openBottom(side: Side): DrawerPlacement? = group(side, bottom = true).firstOrNull { it.open }

    operator fun get(id: String): DrawerPlacement? = drawers.firstOrNull { it.id == id }

    fun toggle(id: String): FrameLayout = if (get(id)?.open == true) close(id) else open(id)

    /** Opens the drawer; a bottom drawer replaces the one open on its side of the bottom panel. */
    fun open(id: String): FrameLayout {
        val drawer = get(id) ?: return this
        return withDrawers(drawers.map {
            when {
                it.id == id -> it.copy(open = true)
                drawer.bottom && it.bottom && it.side == drawer.side -> it.copy(open = false)
                else -> it
            }
        })
    }

    fun close(id: String): FrameLayout = update(id) { it.copy(open = false) }

    fun resize(id: String, width: Float): FrameLayout = update(id) { it.copy(width = width.coerceIn(MIN_WIDTH, MAX_WIDTH)) }

    /** Moves the drawer to the end of [side]'s group, keeping it open or closed. */
    fun dock(id: String, side: Side): FrameLayout {
        val drawer = get(id) ?: return this
        return move(id, side, drawer.bottom, Int.MAX_VALUE)
    }

    /**
     * Moves the drawer to [index] in the rail group given by [side] and [bottom], keeping it open or closed. An open
     * drawer moved into a bottom group replaces the one open there.
     */
    fun move(id: String, side: Side, bottom: Boolean, index: Int): FrameLayout {
        val drawer = get(id) ?: return this
        val rest = drawers.filter { it.id != id }
        val group = rest.filter { it.side == side && it.bottom == bottom }
        val at = index.coerceIn(0, group.size)
        val position = if (at < group.size) rest.indexOf(group[at]) else (rest.indexOfLast { it.side == side && it.bottom == bottom } + 1).takeIf { it > 0 } ?: rest.size
        val moved = drawer.copy(side = side, bottom = bottom)
        val placed = withDrawers(rest.toMutableList().apply { add(position, moved.copy(open = false)) })
        return if (drawer.open) placed.open(id) else placed
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

    fun resizeBottom(height: Float): FrameLayout = copy(bottomHeight = height.coerceIn(MIN_BOTTOM_HEIGHT, MAX_BOTTOM_HEIGHT))

    /** Sets the left half's share of the bottom panel when both halves are open. */
    fun splitBottom(share: Float): FrameLayout = copy(bottomSplit = share.coerceIn(MIN_BOTTOM_SHARE, 1 - MIN_BOTTOM_SHARE))

    private fun withDrawers(list: List<DrawerPlacement>) = copy(drawers = list)

    private fun update(id: String, change: (DrawerPlacement) -> DrawerPlacement): FrameLayout =
        withDrawers(drawers.map { if (it.id == id) change(it) else it })

    companion object {
        const val MIN_WIDTH = 260f
        const val MAX_WIDTH = 420f
        const val DEFAULT_WIDTH = 300f
        const val MIN_SHARE = 0.15f
        const val MIN_BOTTOM_HEIGHT = 140f
        const val MAX_BOTTOM_HEIGHT = 480f
        const val DEFAULT_BOTTOM_HEIGHT = 220f
        const val MIN_BOTTOM_SHARE = 0.25f

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
