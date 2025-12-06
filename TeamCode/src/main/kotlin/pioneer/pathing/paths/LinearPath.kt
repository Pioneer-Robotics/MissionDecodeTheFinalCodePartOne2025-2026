package pioneer.pathing.paths

import pioneer.helpers.Pose

/**
 * LinearPath class representing a straight line path in 2D space
 * @param startPose The starting pose of the path
 * @param endPose The ending pose of the path
 */
class LinearPath(
    override var startPose: Pose = Pose(),
    override var endPose: Pose = Pose(),
) : Path {
    // Constructor overloads
    constructor(startX: Double, startY: Double, endX: Double, endY: Double) : this(Pose(startX, startY), Pose(endX, endY))

    override fun getLength(): Double = startPose.distanceTo(endPose)

    override fun getLengthSoFar(t: Double): Double = getLength() * t

    override fun getTFromLength(length: Double): Double = if (getLength() > 0) length / getLength() else 1.0

    override fun getPoint(t: Double): Pose {
        val x = startPose.x + (endPose.x - startPose.x) * t
        val y = startPose.y + (endPose.y - startPose.y) * t
        return Pose(x, y)
    }

    override fun getPose(t: Double): Pose =
        Pose(
            x = startPose.x + (endPose.x - startPose.x) * t,
            y = startPose.y + (endPose.y - startPose.y) * t,
            vx = (endPose.x - startPose.x) / getLength(),
            vy = (endPose.y - startPose.y) / getLength(),
        )

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
