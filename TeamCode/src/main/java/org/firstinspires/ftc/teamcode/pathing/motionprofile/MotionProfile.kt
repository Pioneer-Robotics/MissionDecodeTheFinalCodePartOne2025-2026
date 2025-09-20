package org.firstinspires.ftc.teamcode.pathing.motionprofile

class MotionProfile(val segments: List<MotionSegment>) {
    init {
        require(segments.isNotEmpty()) { "MotionProfile must have at least one segment." }
    }

    /**
     * Returns the [MotionState] at time [t].
     */
    operator fun get(t: Double): MotionState {
        if (t < 0.0) return segments.first().startState.stationary()

        var remainingTime = t
        for (segment in segments) {
            if (remainingTime <= segment.dt) {
                return segment[remainingTime]
            }
            remainingTime -= segment.dt
        }

        return segments.last().end().stationary()
    }

    /**
     * Returns the duration of the motion profile.
     */
    fun duration() = segments.sumOf { it.dt }

    /**
     * Returns the start [MotionState].
     */
    fun start() = segments.first().startState

    /**
     * Returns the end [MotionState].
     */
    fun end() = segments.last().end()
}