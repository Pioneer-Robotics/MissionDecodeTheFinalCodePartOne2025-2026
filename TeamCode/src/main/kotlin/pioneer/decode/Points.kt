package pioneer.decode

import pioneer.general.AllianceColor
import pioneer.helpers.Pose
import kotlin.math.PI

/*
                            GOAL SIDE
                |-------------------------------|
                |               +Y              |
                |               ^               |
                |               |               |
                |               |               |
BLUE ALLIANCE   |               0----> +X       |    RED ALLIANCE
                |                               |
                |                               |
                |                               |
                |                               |
                |-------------------------------|

                            AUDIENCE SIDE

    THETA = 0 FACING FORWARD
*/

// All points are defined from the RED ALLIANCE perspective
class Points(
    color: AllianceColor,
) {
    // Function to transform a point based on alliance color
    fun Pose.T(c: AllianceColor): Pose =
        when (c) {
            AllianceColor.RED -> this
            AllianceColor.BLUE -> Pose(-this.x, this.y, theta=-this.theta)
            AllianceColor.NEUTRAL -> this
        }

    // Key positions on the field
    // Written in ACTION_POSITION format
    val START_GOAL = Pose(133.0, 134.0, theta = 0.67).T(color)
    val START_FAR = Pose(43.0, -157.0, theta = 0.0).T(color)

    val SHOOT_GOAL_CLOSE = Pose(55.0, 30.0, theta = -PI/2).T(color)
    val SHOOT_GOAL_FAR = Pose(43.0, -140.0, theta = -PI/2).T(color)

    val LEAVE_POSITION = Pose(60.0, -60.0).T(color)

    private fun collectY(i: Int) = 30 - (i * 60.0) // 30, -30, -90

    private val prepCollectX = 78.5
    private val collectTheta = -PI / 2 // Point to right
    val PREP_COLLECT_GOAL = Pose(prepCollectX, collectY(0), theta=collectTheta).T(color)
    val PREP_COLLECT_MID = Pose(prepCollectX, collectY(1), theta=collectTheta).T(color)
    val PREP_COLLECT_AUDIENCE = Pose(prepCollectX, collectY(2), theta=collectTheta).T(color)

    val PREP_COLLECT_START_VELOCITY = Pose(0.0, -75.0).T(color)
    val PREP_COLLECT_END_VELOCITY = Pose(75.0, 0.0).T(color)

    private val collectX = 116.5
    val COLLECT_GOAL = Pose(collectX, collectY(0), theta=collectTheta).T(color)
    val COLLECT_MID = Pose(collectX, collectY(1), theta=collectTheta).T(color)
    val COLLECT_AUDIENCE = Pose(collectX, collectY(2), theta=collectTheta).T(color)
}
