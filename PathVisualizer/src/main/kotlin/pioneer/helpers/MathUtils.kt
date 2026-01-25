package pioneer.helpers

import kotlin.math.*

object MathUtils {

    /**
     * Normalize an angle to the range [-PI, PI]
     */
    fun normalizeAngle(angle: Double): Double {
        var normalized = angle % (2 * PI)
        if (normalized > PI) normalized -= 2 * PI
        if (normalized < -PI) normalized += 2 * PI
        return normalized
    }

    /**
     * Calculate the difference between two angles, normalized to [-PI, PI]
     */
    fun angleDifference(target: Double, current: Double): Double {
        return normalizeAngle(target - current)
    }

    /**
     * Linear interpolation between two values
     */
    fun lerp(start: Double, end: Double, t: Double): Double {
        return start + (end - start) * t
    }

    /**
     * Clamp a value between min and max
     */
    fun clamp(value: Double, min: Double, max: Double): Double {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }

    /**
     * Convert degrees to radians
     */
    fun degreesToRadians(degrees: Double): Double {
        return degrees * PI / 180.0
    }

    /**
     * Convert radians to degrees
     */
    fun radiansToDegrees(radians: Double): Double {
        return radians * 180.0 / PI
    }

    /**
     * Calculate distance between two points
     */
    fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Calculate the sign of a number (-1, 0, or 1)
     */
    fun sign(value: Double): Int {
        return when {
            value > 0 -> 1
            value < 0 -> -1
            else -> 0
        }
    }

    /**
     * Normalize an angle in radians to the range [-PI, PI]
     */
    fun normalizeRadians(angle: Double): Double {
        var normalized = angle % (2 * PI)
        if (normalized > PI) normalized -= 2 * PI
        if (normalized < -PI) normalized += 2 * PI
        return normalized
    }
}