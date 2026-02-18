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
    val START_GOAL = Pose(120.0, 127.5, theta = 0.67 + PI).T(color)
    val START_FAR = Pose(43.0, -157.0, theta = 0.0).T(color)

    val SHOOT_CLOSE = Pose(55.0, 25.0, theta = -PI/2).T(color)
    val SHOOT_FAR = Pose(43.0, -140.0, theta = -PI/2).T(color)

    // Goal mark
    val COLLECT_GOAL_END = Pose(127.5, 30.0, theta = -PI/2).T(color)
    val COLLECT_GOAL_END_VEL = Pose(200.0, 0.0).T(color)
    val COLLECT_GOAL_START_VEL = Pose(0.0, 0.0).T(color)

    // Human player
    val COLLECT_HUMAN_PLAYER = Pose()
    val COLLECT_HUMAN_PLAYER_START_VEL = Pose()
    val COLLECT_HUMAN_PLAYER_END_VEL = Pose()

    // Leave position
    val LEAVE_POSITION = Pose(60.0, -60.0).T(color)
}
