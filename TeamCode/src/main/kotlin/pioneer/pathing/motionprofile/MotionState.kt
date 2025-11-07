package pioneer.pathing.motionprofile

class MotionState(
    val x: Double,
    val v: Double,
    val a: Double,
) {
    /**
     * Returns the [MotionState] at time [t] with constant acceleration.
     */
    operator fun get(t: Double): MotionState {
        return MotionState(
            x + v * t + 0.5 * a * t * t,
            v + a * t,
            a,
        )
    }

    /**
     * Returns the state at t=0 with velocity and acceleration zeroed.
     */
    fun stationary() = MotionState(x, 0.0, 0.0)

    override fun toString(): String {
        return "MotionState(x=$x, v=$v, a=$a)"
    }
}
