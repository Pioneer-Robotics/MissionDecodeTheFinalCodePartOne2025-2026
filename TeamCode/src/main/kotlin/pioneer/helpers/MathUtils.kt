package pioneer.helpers

import kotlin.math.PI

class MathUtils {
    companion object {
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
        fun linspace(start: Double, end: Double, num: Int): List<Double> {
            if (num <= 0) return emptyList()
            if (num == 1) return listOf(start)

            val step = (end - start) / (num - 1)
            return List(num) { i -> start + i * step }
        }
    }
}
