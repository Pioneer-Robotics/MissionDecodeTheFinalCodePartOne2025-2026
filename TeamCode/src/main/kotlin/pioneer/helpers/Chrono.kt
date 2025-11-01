package pioneer.helpers

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

class Chrono(private val source: TimeSource = TimeSource.Monotonic) {
    private var last = source.markNow()

    /** Automatically updates and returns the new delta time in seconds as a Double. */
    val dt: Double
        get() = update().toDouble(DurationUnit.SECONDS)

    /** Compute elapsed time since last update and refresh the reference. */
    private fun update(): Duration {
        val delta = last.elapsedNow()
        last = last + delta
        return delta
    }

    /** Check elapsed time without advancing the mark. */
    fun peek(): Double = last.elapsedNow().toDouble(DurationUnit.SECONDS)

    /** Reset the reference mark to now. */
    fun reset() {
        last = source.markNow()
    }
}