package pioneer.helpers

/**
 * Simple motor feedforward: v + a (+ optionally static)
 * output = kV * velocity + kA * acceleration
 * Units: velocity in cm/s, acceleration in cm/s^2
 */
class Feedforward(val kV: Double, val kA: Double, val kStatic: Double = 0.0) {
    /**
     * Compute feedforward for given velocity and acceleration
     */
    fun calculate(velocity: Double, acceleration: Double): Double {
        val signV = if (velocity >= 0.0) 1.0 else -1.0
        return kV * velocity + kA * acceleration + kStatic * signV
    }

    /**
     * Compute feedforward for an array of wheel velocities & accelerations
     */
    fun calculate(wheelV: DoubleArray, wheelA: DoubleArray? = null): DoubleArray {
        return wheelV.mapIndexed { i, v ->
            val a = wheelA?.get(i) ?: 0.0
            calculate(v, a)
        }.toDoubleArray()
    }
}
