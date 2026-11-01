package dev.chieppa.stretch.input

enum class PointerKind { Mouse, Pen, Eraser, Touch }

enum class PointerPhase { Hover, Down, Move, Up, Leave }

/**
 * One pointer position in window client pixels. Pen-only values are null when the device does not report them;
 * they are never faked. Pressure is 0..1, tilt in degrees. [timeNanos] comes from a monotonic high-resolution clock.
 * [coalesced] marks a sample the platform batched before the newest one.
 */
data class PointerSample(
    val x: Float,
    val y: Float,
    val phase: PointerPhase,
    val kind: PointerKind,
    val pressure: Float? = null,
    val tiltX: Float? = null,
    val tiltY: Float? = null,
    val barrel: Boolean = false,
    val timeNanos: Long = 0,
    val coalesced: Boolean = false,
)

/**
 * Pen samples read from the platform, queued as they arrive and drained by the tool that consumes them, usually once
 * per frame. Mouse and touch keep arriving through the UI toolkit as usual.
 */
interface PenSource : AutoCloseable {
    /** Starts listening; safe to call again when the window gains child windows. */
    fun attach()

    /** The next queued sample, or null when the queue is empty. */
    fun poll(): PointerSample?

    fun drain(): List<PointerSample> = generateSequence { poll() }.toList()
}

/** Maps one axis linearly, such as pen device coordinates to screen pixels. */
data class AxisMapping(val fromStart: Float, val fromEnd: Float, val toStart: Float, val toEnd: Float) {
    init {
        require(fromEnd != fromStart) { "Empty source range" }
    }

    fun map(value: Float): Float = toStart + (value - fromStart) * (toEnd - toStart) / (fromEnd - fromStart)
}

/** The phase of a pen sample from its contact state now and at the previous sample. */
fun penPhase(inContact: Boolean, wasInContact: Boolean, left: Boolean = false): PointerPhase = when {
    left -> PointerPhase.Leave
    inContact && !wasInContact -> PointerPhase.Down
    inContact -> PointerPhase.Move
    wasInContact -> PointerPhase.Up
    else -> PointerPhase.Hover
}
