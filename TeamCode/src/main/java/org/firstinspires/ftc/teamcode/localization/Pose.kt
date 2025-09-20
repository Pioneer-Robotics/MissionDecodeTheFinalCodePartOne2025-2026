package org.firstinspires.ftc.teamcode.localization

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Represents a 2D pose with x, y coordinates and a heading (angle).
 * Provides methods for vector operations, distance calculations, and equality checks.
 */
class Pose(var x: Double = 0.0, var y: Double = 0.0, var heading: Double = 0.0) {
    // Vector operations
    fun add(other: Pose) {
        this.x += other.x
        this.y += other.y
        this.heading += other.heading
    }

    fun subtract(other: Pose) {
        this.x -= other.x
        this.y -= other.y
        this.heading -= other.heading
    }

    fun scale(scalar: Double, scaleHeading: Boolean = false) {
        this.x *= scalar
        this.y *= scalar
        if (scaleHeading) {
            this.heading *= scalar
        }
    }

    fun divide(scalar: Double) {
        if (scalar != 0.0) {
            this.x /= scalar
            this.y /= scalar
            this.heading /= scalar
        } else {
            throw IllegalArgumentException("Cannot divide by zero")
        }
    }

    fun rotate(angle: Double) {
        // Rotate about the origin (0, 0)
        val cosTheta = cos(angle)
        val sinTheta = sin(angle)
        val newX = this.x * cosTheta - this.y * sinTheta
        val newY = this.x * sinTheta + this.y * cosTheta
        this.x = newX
        this.y = newY
    }

    fun normalize() : Pose {
        val length = this.getLength()
        return if (length > 0) {
            Pose(this.x / length, this.y / length, this.heading)
        } else {
            Pose(0.0, 0.0, this.heading) // Return a zero vector with the same heading
        }
    }

    // Overloaded operators for vector operations
    operator fun plus(other: Pose): Pose {
        return Pose(this.x + other.x, this.y + other.y, this.heading + other.heading)
    }

    operator fun minus(other: Pose): Pose {
        return Pose(this.x - other.x, this.y - other.y, this.heading - other.heading)
    }

    operator fun times(scalar: Double): Pose {
        return Pose(this.x * scalar, this.y * scalar, this.heading * scalar)
    }

    operator fun times(otherPose: Pose): Pose {
        // Element-wise multiplication
        return Pose(this.x * otherPose.x, this.y * otherPose.y, this.heading * otherPose.heading)
    }

    operator fun div(scalar: Double): Pose {
        if (scalar != 0.0) {
            return Pose(this.x / scalar, this.y / scalar, this.heading / scalar)
        } else {
            throw IllegalArgumentException("Cannot divide by zero")
        }
    }

    // Other utility methods
    fun getLength(): Double {
        return sqrt(x * x + y * y)
    }

    fun distanceTo(other: Pose): Double {
        return sqrt((other.x - this.x).pow(2.0) + (other.y - this.y).pow(2.0))
    }

    fun roughlyEquals(other: Pose, positionTolerance: Double = 0.001, headingTolerance: Double = 0.001): Boolean {
        return this.distanceTo(other) < positionTolerance &&
                kotlin.math.abs(this.heading - other.heading) < headingTolerance
    }

    override fun toString(): String {
        return "(x=${"%.3f".format(x)}, y=${"%.3f".format(y)}, heading=${"%.3f".format(heading)})"
    }

    fun toDesmosString(): String {
        return "(${"%.3f".format(x)}, ${"%.3f".format(y)})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Pose) return false
        return this.x == other.x && this.y == other.y && this.heading == other.heading
    }

    fun copy(): Pose {
        return Pose(x, y, heading)
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + heading.hashCode()
        return result
    }
}
