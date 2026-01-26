package pioneer.pathing.follower

import kotlin.math.abs

/**
 * Feedforward for X (forward), Y (strafe), and Theta (rotation).
 * All units:
 *  - linear: cm/s, cm/s^2
 *  - angular: rad/s, rad/s^2
 */
class RobotFeedforward(
    val kVX: Double, val kAX: Double, val kVY: Double, val kAY: Double,
    val kVTheta: Double, val kATheta: Double,
    val kStaticX: Double = 0.0,
    val kStaticY: Double = 0.0,
    val kStaticTheta: Double = 0.0
) {
    fun calculateX(v: Double, a: Double): Double {
        val signV = if (v >= 0.0) 1.0 else -1.0
        return kVX * v + kAX * a + kStaticX * if (abs(v) > 1e3) signV else 0.0
    }

    fun calculateY(v: Double, a: Double): Double {
        val signV = if (v >= 0.0) 1.0 else -1.0
        return kVY * v + kAY * a + kStaticY * if (abs(v) > 1e3) signV else 0.0
    }

    fun calculateTheta(v: Double, a: Double): Double {
        val signV = if (v >= 0.0) 1.0 else -1.0
        return kVTheta * v + kATheta * a + kStaticTheta * if (abs(v) > 1e3) signV else 0.0
    }
}
