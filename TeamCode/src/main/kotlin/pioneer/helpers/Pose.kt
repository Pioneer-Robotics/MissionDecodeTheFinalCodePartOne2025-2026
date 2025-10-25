package pioneer.helpers

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Minimal 2D pose with motion derivatives. All units are in centimeters and radians.
 */
data class Pose(
    // Position
    val x: Double = 0.0,
    val y: Double = 0.0,
    // Velocity
    val vx: Double = 0.0,
    val vy: Double = 0.0,
    // Acceleration
    val ax: Double = 0.0,
    val ay: Double = 0.0,
    // Heading and angular derivatives
    val theta: Double = 0.0,
    val omega: Double = 0.0,
    val alpha: Double = 0.0,
) {
    // Angle wrap to (-π, π] using atan2(sin,cos) for numeric stability
    private fun wrap(a: Double): Double = atan2(sin(a), cos(a))

    fun normalize(): Pose = copy(theta = wrap(theta))

    // Constant accel and angular accel over dt
    fun integrate(dt: Double): Pose {
        require(dt.isFinite()) { "dt must be finite" }
        val dt2 = dt * dt
        return copy(
            x = x + vx * dt + 0.5 * ax * dt2,
            y = y + vy * dt + 0.5 * ay * dt2,
            vx = vx + ax * dt,
            vy = vy + ay * dt,
            theta = wrap(theta + omega * dt + 0.5 * alpha * dt2),
            omega = omega + alpha * dt
        )
    }

    // Time derivative assuming constant accel terms
    fun derivative(): Pose = Pose(
        vx, vy,
        ax, ay,
        0.0, 0.0,
        omega, alpha, 0.0
    )

    // Metrics
    fun getLength(): Double = hypot(x, y)
    infix fun distanceTo(other: Pose): Double = hypot(other.x - x, other.y - y)
    infix fun angleTo(other: Pose): Double = atan2(other.y - y, other.x - x)

    fun roughlyEquals(
        other: Pose,
        positionTolerance: Double = 0.01,
        angleTolerance: Double = 1e-3
    ): Boolean =
        distanceTo(other) < positionTolerance &&
            abs(wrap(theta - other.theta)) < angleTolerance

    fun rotate(angle: Double, origin: Pose = Pose()): Pose {
        val cosA = cos(angle)
        val sinA = sin(angle)
        val dx = x - origin.x
        val dy = y - origin.y
        return copy(
            x = origin.x + dx * cosA - dy * sinA,
            y = origin.y + dx * sinA + dy * cosA,
            vx = vx * cosA - vy * sinA,
            vy = vx * sinA + vy * cosA,
            ax = ax * cosA - ay * sinA,
            ay = ax * sinA + ay * cosA,
            theta = wrap(theta + angle)
        )
    }

    // Minimal linear ops (θ wrapped)
    operator fun plus(o: Pose): Pose = copy(
        x = x + o.x, y = y + o.y,
        vx = vx + o.vx, vy = vy + o.vy,
        ax = ax + o.ax, ay = ay + o.ay,
        theta = wrap(theta + o.theta),
        omega = omega + o.omega,
        alpha = alpha + o.alpha
    )

    operator fun minus(o: Pose): Pose = copy(
        x = x - o.x, y = y - o.y,
        vx = vx - o.vx, vy = vy - o.vy,
        ax = ax - o.ax, ay = ay - o.ay,
        theta = wrap(theta - o.theta),
        omega = omega - o.omega,
        alpha = alpha - o.alpha
    )

    operator fun times(s: Double): Pose = copy(
        x = x * s, y = y * s,
        vx = vx * s, vy = vy * s,
        ax = ax * s, ay = ay * s,
        theta = wrap(theta * s),
        omega = omega * s,
        alpha = alpha * s
    )

    operator fun div(s: Double): Pose {
        require(s != 0.0) { "Cannot divide by zero" }
        return copy(
            x = x / s, y = y / s,
            vx = vx / s, vy = vy / s,
            ax = ax / s, ay = ay / s,
            theta = wrap(theta / s),
            omega = omega / s,
            alpha = alpha / s
        )
    }

    operator fun unaryMinus(): Pose = copy(
        x = -x, y = -y,
        vx = -vx, vy = -vy,
        ax = -ax, ay = -ay,
        theta = -theta,
        omega = -omega,
        alpha = -alpha
    )

    override fun toString(): String {
        val th = wrap(theta)
        return "Pose(x=${"%.3f".format(x)}, y=${"%.3f".format(y)}, " +
               "vx=${"%.3f".format(vx)}, vy=${"%.3f".format(vy)}, " +
               "ax=${"%.3f".format(ax)}, ay=${"%.3f".format(ay)}, " +
               "θ=${"%.3f".format(th)}, ω=${"%.3f".format(omega)}, α=${"%.3f".format(alpha)})"
    }

    fun toDesmosString(): String = "(${"%.3f".format(x)}, ${"%.3f".format(y)})"
}