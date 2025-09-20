package org.firstinspires.ftc.teamcode.pathing.motionprofile

class MotionSegment(
    val startState: MotionState,
    val dt: Double
) {
    /**
     * Returns the [MotionState] at time [t].
     */
    operator fun get(t: Double) = startState[t]

    /**
     * Returns the [MotionState] at the end of the segment (time [dt]).
     */
    fun end() = startState[dt]

    override fun toString(): String {
        return "MotionSegment(startState=$startState, dt=$dt)"
    }
}
