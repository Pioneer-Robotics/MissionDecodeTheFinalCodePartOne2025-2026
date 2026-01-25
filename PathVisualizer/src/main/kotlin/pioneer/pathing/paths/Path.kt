package pioneer.pathing.paths

import pioneer.helpers.Pose

/**
 * Path interface representing a path in 2D space
 */
interface Path {
    /**
     * The start and end poses of the path
     */
    var startPose: Pose

    /**
     * The end pose of the path
     */
    var endPose: Pose

    /**
     * Gets the arc length of the path
     * @return The length of the path
     */
    fun getLength(): Double

    /**
     * Gets the pose on the path at the given parameter t
     * @return Pose object with position, velocity, and acceleration
     */
    fun getPose(t: Double): Pose

    /**
     * Gets the arc length from the start of the path to the given parameter t
     * @param t The parameter value in the range [0, 1]
     * @return The length of the path from the start to the given parameter
     */
    fun getLengthSoFar(t: Double): Double

    /**
     * Gets the parameter t value from the given arc length
     * @param length The arc length
     * @return The parameter value in the range [0, 1]
     */
    fun getTFromLength(length: Double): Double

    /**
     * Gets the point at the given parameter t
     * @param t The parameter value in the range [0, 1]
     * @return The point at the given parameter
     */
    fun getPoint(t: Double): Pose

    /**
     * Gets the curvature at the given parameter t
     * @param t The parameter value in the range [0, 1]
     * @return The curvature at the given parameter
     */
    fun getCurvature(t: Double): Double

    /**
     * Gets the closest point t value to the given position on the path
     * @param position The position to get the closest point to
     * @return The parameter t value of the closest point on the path to the given position
     */
    fun getClosestPointT(position: Pose): Double

    /**
     * Gets the closest point on the path to the given position
     * @param position The position to get the closest point to
     * @return The closest point on the path to the given position
     */
    fun getClosestPoint(position: Pose): Pose = getPoint(getClosestPointT(position))
}
