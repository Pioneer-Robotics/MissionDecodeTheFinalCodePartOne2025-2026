package pioneer.helpers

import kotlin.time.DurationUnit
import kotlin.time.TimeSource

/**
 * A utility class for measuring elapsed time. This class provides functionality to track time intervals
 * and optionally updates the elapsed time automatically on each access.
 *
 * @property autoUpdate If true, the elapsed time (`dt`) is updated automatically on each access.
 *                      This should only be used when there is a single consumer of `dt`.
 * @property source The time source used for measuring elapsed time. Defaults to `TimeSource.Monotonic`.
 */
class Chrono(
    private val autoUpdate: Boolean = true,
    private val source: TimeSource = TimeSource.Monotonic,
    private val units: DurationUnit = DurationUnit.SECONDS,
) {
    private var last = source.markNow()

    /** Returns the delta time. Updates automatically if autoUpdate is true. */
    val dt: Double
        get() = if (autoUpdate) update() else peek()

    /** Compute elapsed time since last update and refresh the reference. */
    fun update(): Double {
        val delta = last.elapsedNow()
        reset()
        return delta.toDouble(units)
    }

    /** Check elapsed time without advancing the mark. */
    fun peek(): Double = last.elapsedNow().toDouble(units)

    /** Reset the reference mark to now. */
    fun reset() {
        last = source.markNow()
    }
}
