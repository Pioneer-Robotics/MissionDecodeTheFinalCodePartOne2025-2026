package org.firstinspires.ftc.teamcode.helpers

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

        fun linspace(start: Double, end: Double, num: Int): List<Double> {
            if (num <= 0) return emptyList()
            if (num == 1) return listOf(start)

            val step = (end - start) / (num - 1)
            return List(num) { i -> start + i * step }
        }
    }
}
