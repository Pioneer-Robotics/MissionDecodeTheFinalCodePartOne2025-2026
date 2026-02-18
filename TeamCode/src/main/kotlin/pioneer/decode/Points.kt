package pioneer.decode

import pioneer.general.AllianceColor
import pioneer.helpers.Pose
import pioneer.pathing.motionprofile.constraints.VelocityConstraint
import pioneer.pathing.paths.HermitePath
import pioneer.pathing.paths.Path
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
    val color: AllianceColor,
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

    // Leave position
    val LEAVE_POSITION = Pose(60.0, -60.0).T(color)

    // More complex paths
    fun PATH_HUMAN_PLAYER(startPose: Pose): Path {
        return HermitePath.Builder()
            .addPoint(startPose, Pose(0.0, 100.0).T(color))
            .addPoint(Pose(100.0, -110.0, theta = -6.0 * PI / 7.0).T(color))
            .addPoint(Pose(148.0, -150.0, theta = -6.0 * PI / 7.0).T(color), Pose(-50.0, -300.0).T(color))
            .build()
            .apply {
                // Slow down near the end of the path
                velocityConstraint = VelocityConstraint { s ->
                    if (s > this.getLength() - 40.0) 15.0 else Double.MAX_VALUE
                }
            }
    }

    fun PATH_COLLECT_AUDIENCE(startPose: Pose) : Path {
        return HermitePath.Builder()
            .addPoint(startPose, Pose(0.0, 100.0).T(color))
            .addPoint(Pose(78.5, -90.0, theta = -PI/2).T(color), Pose(-100.0, 0.0).T(color))
            .addPoint(Pose(127.5, -90.0, theta = -PI/2).T(color))
            .build()
    }
}
