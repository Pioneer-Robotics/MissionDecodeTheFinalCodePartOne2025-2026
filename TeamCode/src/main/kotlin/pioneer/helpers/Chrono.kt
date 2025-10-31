package pioneer.helpers

import kotlin.time.Duration
import kotlin.time.TimeSource

class Chrono(private val source: TimeSource = TimeSource.Monotonic) {
    private var last = source.markNow()

    /** Automatically updates and returns the new delta time. */
    val dt: Duration
        get() = update()

    /** Compute elapsed time since last update and refresh the reference. */
    fun update(): Duration {
        val delta = last.elapsedNow()
        last = last + delta
        return delta
    }

    /** Check elapsed time without advancing the mark. */
    fun peek(): Duration = last.elapsedNow()

    /** Reset the reference mark to now. */
    fun reset() {
        last = source.markNow()
    }
}