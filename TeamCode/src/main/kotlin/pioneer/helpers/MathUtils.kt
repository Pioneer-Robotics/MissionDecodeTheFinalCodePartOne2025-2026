package pioneer.helpers

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MathUtils {
    /**
     * Normalizes an angle to the range (-π, π].
     * @param angle The angle in radians to normalize.
     * @return The normalized angle in radians.
     */
    fun normalizeRadians(angle: Double): Double {
        var normalized = angle
        while (normalized > PI) normalized -= 2 * PI
        while (normalized <= -PI) normalized += 2 * PI
        return normalized
    }

    fun normalizeRadians(
        angle: Double,
        range: Pair<Double, Double>,
    ): Double {
        var normalized = angle
        while (normalized > range.second) normalized -= 2 * PI
        while (normalized <= range.first) normalized += 2 * PI
        return normalized
    }

    /**
     * Creates a linearly spaced array of values.
     * @param start Starting value
     * @param end Ending value (inclusive)
     * @param num Number of points to generate
     * @return List of evenly spaced values
     */
    fun linspace(
        start: Double,
        end: Double,
        num: Int,
    ): List<Double> {
        if (num <= 0) return emptyList()
        if (num == 1) return listOf(start)

        val step = (end - start) / (num - 1)
        return List(num) { i -> start + i * step }
    }

    fun inToCM(inch: Double): Double = (inch * 2.54)

    /**
     * Rotates a 2D vector by a given heading angle.
     * @param x X component of the vector
     * @param y Y component of the vector
     * @param heading Angle in radians to rotate the vector
     * @return Pair of rotated (x, y) components
     */
    fun rotateVector(
        x: Double,
        y: Double,
        heading: Double,
    ): Pair<Double, Double> {
        val cos = cos(heading)
        val sin = sin(heading)
        return Pair(
            x * cos - y * sin,
            x * sin + y * cos,
        )
    }

    fun quaternionToEuler(q: Quaternion): YawPitchRollAngles {
        val qw = q.w.toDouble()
        val qx = q.x.toDouble()
        val qy = q.y.toDouble()
        val qz = q.z.toDouble()

        val yaw = atan2(2 * (qw * qz + qx * qy), 1 - 2 * (qy * qy + qz * qz))
        val pitch =
            atan2(
                sqrt(1 + 2 * (qw * qy - qx * qz)),
                sqrt(1 - 2 * (qw * qy - qx * qz)),
            ) - PI / 2
        val roll = atan2(2 * (qw * qx + qy * qz), 1 - 2 * (qx * qx + qy * qy))

        return YawPitchRollAngles(AngleUnit.RADIANS, yaw, pitch, roll, 0)
    }
}
