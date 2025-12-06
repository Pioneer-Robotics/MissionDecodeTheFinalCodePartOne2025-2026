package pioneer.helpers

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
}
