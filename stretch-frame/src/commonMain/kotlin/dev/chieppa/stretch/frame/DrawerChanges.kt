package dev.chieppa.stretch.frame

/**
 * Adds a drawer an app offers while it runs. One that was here before and taken out with [FrameLayout.removeDrawer]
 * comes back where it was, open, closed or in its window as it was then; a new one takes [placement], at the end of
 * its rail group. A drawer already in the layout stays as it is.
 */
fun FrameLayout.addDrawer(placement: DrawerPlacement): FrameLayout {
    if (get(placement.id) != null) return this
    val place = removed[placement.id] ?: placement
    val rest = removed - placement.id
    val group = drawers.indexOfLast { it.side == place.side && it.inRailFoot == place.inRailFoot }
    val at = if (group >= 0) group + 1 else drawers.size
    val added = copy(removed = rest).withDrawers(drawers.toMutableList().apply { add(at, place.copy(open = false, detached = false)) })
    return when {
        place.detached -> added.popOut(place.id)
        place.open -> added.open(place.id)
        else -> added
    }
}

/** Takes a drawer out of the layout, its window included, remembering where it was for when it comes back. */
fun FrameLayout.removeDrawer(id: String): FrameLayout {
    val placement = get(id) ?: return this
    return copy(drawers = drawers.filter { it.id != id }, removed = removed + (id to placement))
}

/**
 * The layout holding exactly the drawers in [ids]: those not in it are added with [placeNew] (or where they were
 * before), and those no longer offered are taken out and remembered.
 */
fun FrameLayout.syncDrawers(ids: Collection<String>, placeNew: (String) -> DrawerPlacement): FrameLayout {
    val gone = drawers.map { it.id }.filter { it !in ids }
    val trimmed = gone.fold(this) { layout, id -> layout.removeDrawer(id) }
    return ids.fold(trimmed) { layout, id -> if (layout[id] != null) layout else layout.addDrawer(placeNew(id)) }
}
