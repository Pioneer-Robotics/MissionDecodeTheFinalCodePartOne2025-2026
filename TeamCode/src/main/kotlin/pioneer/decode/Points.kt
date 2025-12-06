package pioneer.decode

import pioneer.general.AllianceColor
import pioneer.helpers.Pose
import kotlin.math.PI
import kotlin.math.sqrt

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
            AllianceColor.BLUE -> Pose(-this.x, this.y, -this.theta)
            AllianceColor.NEUTRAL -> this
        }

    // Key positions on the field
    // Written in ACTION_POSITION format
    val START_GOAL = Pose(130.0, 137.0, theta = 3 * PI / 4).T(color)

    val SHOOT_GOAL_CLOSE = Pose(60.0, 60.0, theta = 0.0).T(color)

    private fun collectY(i: Int) = 30 - (i * 60.0) // 30, -30, -90

    private val prepCollectX = 70.0
    private val collectTheta = -PI / 2 // Point to right
    val PREP_COLLECT_GOAL = Pose(prepCollectX, collectY(0), collectTheta).T(color)
    val PREP_COLLECT_MID = Pose(prepCollectX, collectY(1), collectTheta).T(color)
    val PREP_COLLECT_AUDIENCE = Pose(prepCollectX, collectY(2), collectTheta).T(color)

    private val collectX = 130.0
    val COLLECT_GOAL = Pose(collectX, collectY(0), collectTheta).T(color)
    val COLLECT_MID = Pose(collectX, collectY(1), collectTheta).T(color)
    val COLLECT_AUDIENCE = Pose(collectX, collectY(2), collectTheta).T(color)

    // Half the goal depth (46.45 cm)
    val shootingOffset: Pose = Pose(x = (46.45 / 2)/sqrt(2.0), y = (46.45 / 2)/sqrt(2.0)).T(color)
}
