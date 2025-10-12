package org.firstinspires.ftc.teamcode.localization

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Represents a 2D pose with x, y coordinates and a heading (angle).
 * Provides methods for vector operations, distance calculations, and equality checks.
 */
data class Pose(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val heading: Double = 0.0,
) {
    fun rotate(angle: Double): Pose {
        // Rotate the (x, y) vector by the given angle in radians
        val cosTheta = cos(angle)
        val sinTheta = sin(angle)
        val newX = x * cosTheta - y * sinTheta
        val newY = x * sinTheta + y * cosTheta
        return Pose(newX, newY, heading)
    }

    fun normalize(): Pose {
        val len = length
        return if (len > 0) {
            Pose(x / len, y / len, heading)
        } else {
            Pose(0.0, 0.0, heading) // Return a zero vector with the same heading
        }
    }

    // Overloaded operators for vector operations
    operator fun plus(other: Pose): Pose = Pose(x + other.x, y + other.y, heading + other.heading)

    operator fun minus(other: Pose): Pose = Pose(x - other.x, y - other.y, heading - other.heading)

    operator fun times(scalar: Double): Pose = Pose(x * scalar, y * scalar, heading)

    operator fun div(scalar: Double): Pose {
        require(scalar != 0.0) { "Cannot divide by zero" }
        return Pose(x / scalar, y / scalar, heading)
    }

    // Other utility methods
    val length: Double get() = sqrt(x * x + y * y)

    fun distanceTo(other: Pose): Double = hypot(other.x - x, other.y - y)

    fun angleTo(other: Pose): Double = atan2(other.y - y, other.x - x)

    fun roughlyEquals(
        other: Pose,
        positionTolerance: Double = 0.001,
        headingTolerance: Double = 0.001,
    ): Boolean =
        distanceTo(other) < positionTolerance &&
            abs(heading - other.heading) < headingTolerance

    override fun toString(): String = "Pose(x=${"%.3f".format(x)}, y=${"%.3f".format(y)}, heading=${"%.3f".format(heading)})"

    fun toDesmosString(): String = "(${"%.3f".format(x)}, ${"%.3f".format(y)})"
}
