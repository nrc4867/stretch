package dev.chieppa.stretch.frame

enum class Side { Left, Right }

/**
 * Where a drawer is showing, so its contents can lay out for a tall column, a wide strip or a window of its own, which
 * the person sizes freely.
 */
enum class DrawerPosition { Side, Bottom, Window }

/** A popped-out drawer's window on screen, in dp. */
data class WindowBounds(val x: Float, val y: Float, val width: Float, val height: Float)

/**
 * Where one drawer sits: its side, its order on that side, its width in dp and whether it is open. [bottom] puts its
 * rail button in the group at the foot of the rail and opens it in the bottom panel instead of the side column.
 * [height] is its share of the side column when drawers stack. [railTop] keeps a [bottom] drawer's rail button in the
 * top group, for an app whose bottom panel belongs with its side drawers. A [detached] drawer shows in a window of its own,
 * placed at [window]; its rail button stays where it was, so it docks back there. [window] is kept after docking, so
 * popping out again reopens it in the same place.
 */
data class DrawerPlacement(
    val id: String,
    val side: Side,
    val width: Float = FrameLayout.DEFAULT_WIDTH,
    val open: Boolean = false,
    val bottom: Boolean = false,
    val height: Float = 1f,
    val detached: Boolean = false,
    val window: WindowBounds? = null,
    val railTop: Boolean = false,
) {
    /** Whether its rail button sits in the group at the foot of the rail. */
    val inRailFoot: Boolean get() = bottom && !railTop

    /** The width of its window, in dp. */
    val windowWidth: Float get() = window?.width ?: width

    /** The height of its window, in dp. */
    val windowHeight: Float get() = window?.height ?: FrameLayout.DEFAULT_WINDOW_HEIGHT

    val position: DrawerPosition
        get() = when {
            detached -> DrawerPosition.Window
            bottom -> DrawerPosition.Bottom
            else -> DrawerPosition.Side
        }
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
        require(drawers.none { it.detached && it.open }) { "A popped-out drawer is not open in the frame" }
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

    /** Drawers showing in windows of their own. */
    val detached: List<DrawerPlacement> get() = drawers.filter { it.detached }

    operator fun get(id: String): DrawerPlacement? = drawers.firstOrNull { it.id == id }

    fun toggle(id: String): FrameLayout = if (get(id)?.open == true) close(id) else open(id)

    /**
     * Opens the drawer; a bottom drawer replaces the one open on its side of the bottom panel. A popped-out drawer
     * docks back to open.
     */
    fun open(id: String): FrameLayout {
        val drawer = get(id) ?: return this
        return withDrawers(drawers.map {
            when {
                it.id == id -> it.copy(open = true, detached = false)
                drawer.bottom && it.bottom && it.side == drawer.side -> it.copy(open = false)
                else -> it
            }
        })
    }

    fun close(id: String): FrameLayout = update(id) { it.copy(open = false) }

    /** Takes the drawer out of the frame into a window of its own. Its rail button stays, marking where it docks. */
    fun popOut(id: String): FrameLayout = update(id) { it.copy(open = false, detached = true) }

    /** Docks a popped-out drawer back where its rail button is, open. */
    fun dockBack(id: String): FrameLayout = if (get(id)?.detached == true) open(id) else this

    /** Remembers where a popped-out drawer's window is. */
    fun moveWindow(id: String, bounds: WindowBounds): FrameLayout = update(id) { it.copy(window = bounds) }

    fun resize(id: String, width: Float): FrameLayout = update(id) { it.copy(width = width.coerceIn(MIN_WIDTH, MAX_WIDTH)) }

    /** Moves the drawer to the end of [side]'s group, keeping it open or closed. */
    fun dock(id: String, side: Side): FrameLayout {
        val drawer = get(id) ?: return this
        return move(id, side, drawer.bottom, Int.MAX_VALUE)
    }

    /**
     * Moves the drawer to [index] in the rail group given by [side] and [bottom], keeping it open or closed. An open
     * drawer moved into a bottom group replaces the one open there. A popped-out drawer docks there, open.
     */
    fun move(id: String, side: Side, bottom: Boolean, index: Int): FrameLayout {
        val drawer = get(id) ?: return this
        val rest = drawers.filter { it.id != id }
        val group = rest.filter { it.side == side && it.inRailFoot == bottom }
        val at = index.coerceIn(0, group.size)
        val position = if (at < group.size) rest.indexOf(group[at]) else (rest.indexOfLast { it.side == side && it.inRailFoot == bottom } + 1).takeIf { it > 0 } ?: rest.size
        val moved = if (drawer.railTop && !bottom) {
            drawer.copy(side = side, detached = false)
        } else {
            drawer.copy(side = side, bottom = bottom, railTop = false, detached = false)
        }
        val placed = withDrawers(rest.toMutableList().apply { add(position, moved.copy(open = false)) })
        return if (drawer.open || drawer.detached) placed.open(id) else placed
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

    /**
     * The scale for side drawer widths so that, in a row [width] dp wide, the viewport keeps [MIN_VIEWPORT]. 1 when
     * they fit. Only drawers in [shown] count. The stored widths stay as they are, so a wider window restores them.
     */
    fun sideFit(width: Float, shown: Set<String>): Float {
        val sides = Side.entries.sumOf { side -> (openOn(side).firstOrNull { it.id in shown }?.width ?: 0f).toDouble() }.toFloat()
        val room = width - MIN_VIEWPORT
        return if (sides <= room || sides <= 0f) 1f else (room / sides).coerceAtLeast(0f)
    }

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
        const val DEFAULT_WINDOW_HEIGHT = 520f

        /** The narrowest the viewport gets while side drawers are open; past it the drawers are shown narrower. */
        const val MIN_VIEWPORT = 240f
    }
}

/** Each mode's layout, starting from the mode's default and remembered until reset. */
data class ModeLayouts(val defaults: Map<String, FrameLayout>, val current: Map<String, FrameLayout> = defaults) {
    operator fun get(mode: String): FrameLayout = current[mode] ?: defaults.getValue(mode)

    fun update(mode: String, change: (FrameLayout) -> FrameLayout): ModeLayouts =
        copy(current = current + (mode to change(get(mode))))

    fun reset(mode: String): ModeLayouts = copy(current = current + (mode to defaults.getValue(mode)))
}
