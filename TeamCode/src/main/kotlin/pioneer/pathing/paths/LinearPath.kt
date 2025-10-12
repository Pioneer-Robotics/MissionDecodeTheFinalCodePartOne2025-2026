package pioneer.pathing.paths

import pioneer.helpers.MathUtils
import pioneer.localization.Pose
import pioneer.pathing.paths.Path.HeadingInterpolationMode

/**
 * LinearPath class representing a straight line path in 2D space
 * @param startPose The starting pose of the path
 * @param endPose The ending pose of the path
 */
class LinearPath (override var startPose: Pose = Pose(), override var endPose: Pose = Pose()) :
    Path {
    // Constructor overloads
    constructor(startX: Double, startY: Double, endX: Double, endY: Double) : this(Pose(startX, startY), Pose(endX, endY))

    // Enum for heading interpolation mode
    override var headingInterpolationMode: HeadingInterpolationMode = HeadingInterpolationMode.LINEAR

    override fun getLength(): Double {
        return startPose.distanceTo(endPose)
    }

    override fun getLengthSoFar(t: Double): Double {
        return getLength() * t
    }

    override fun getTFromLength(length: Double): Double {
        return length / getLength()
    }

    override fun getHeading(t: Double): Double {
        when (headingInterpolationMode) {
            HeadingInterpolationMode.LINEAR -> {
                val delta = MathUtils.normalizeRadians(endPose.heading - startPose.heading)
                return startPose.heading + delta * t
            }
        }
    }

    override fun getPoint(t: Double): Pose {
        val x = startPose.x + (endPose.x - startPose.x) * t
        val y = startPose.y + (endPose.y - startPose.y) * t
        val heading = getHeading(t)
        return Pose(x, y, heading)
    }

    override fun getTangent(t: Double): Pose {
        val dx = endPose.x - startPose.x
        val dy = endPose.y - startPose.y
        val length = getLength()
        return if (length != 0.0) {
            Pose(dx / length, dy / length)
        } else {
            Pose(0.0, 0.0)
        }
    }

    override fun getNormal(t: Double): Pose {
        val tangent = getTangent(t)
        return Pose(-tangent.y, tangent.x)
    }

    override fun getSecondDerivative(t: Double): Pose {
        return Pose(0.0, 0.0) // Linear paths have zero second derivative
    }

    override fun getCurvature(t: Double): Double {
        return 0.0 // Linear paths have zero curvature
    }

    override fun getClosestPointT(position: Pose): Double {
        val dx = endPose.x - startPose.x
        val dy = endPose.y - startPose.y
        val fx = position.x - startPose.x
        val fy = position.y - startPose.y

        // Calculate the projection of (fx, fy) onto (dx, dy)
        val dotProduct = fx * dx + fy * dy
        val lengthSquared = dx * dx + dy * dy

        if (lengthSquared == 0.0) {
            return 0.0 // The path is a point
        }

        var t = dotProduct / lengthSquared
        t = t.coerceIn(0.0, 1.0) // Coerce to [0, 1]

        return t
    }

    class Builder {
        private val points = mutableListOf<Pose>()

        fun addPoint(point: Pose): Builder {
            points.add(point)
            return this
        }

        fun addPoints(vararg newPoints: Pose): Builder {
            points.addAll(newPoints)
            return this
        }

        /**
         * Builds a LinearPath or CompoundPath based on the number of points added.
         * If only two points are added, a LinearPath is created.
         * If more than two points are added, a CompoundPath is created with LinearPaths between each pair of points.
         * @return A LinearPath or CompoundPath based on the points added
         */
        fun build(): Path {
            if (points.size < 2) {
                throw IllegalArgumentException("At least two points are required to create a LinearPath")
            }
            if (points.size == 2) {
                return LinearPath(points[0], points[1])
            } else {
                val paths = mutableListOf<Path>()
                for (i in 0 until points.size - 1) {
                    paths.add(LinearPath(points[i], points[i + 1]))
                }
                return CompoundPath(paths)
            }
        }
    }
}
