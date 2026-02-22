package pioneer.decode

import pioneer.general.AllianceColor
import pioneer.helpers.Pose
import pioneer.pathing.motionprofile.constraints.VelocityConstraint
import pioneer.pathing.paths.CompoundPath
import pioneer.pathing.paths.HermitePath
import pioneer.pathing.paths.LinearPath
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
    val START_GOAL = Pose(135.0, 128.0, theta = 0.67 + PI).T(color)
    val START_FAR = Pose(43.0, -157.0, theta = 0.0).T(color)

    val SHOOT_CLOSE = Pose(50.0, 25.0, theta = -PI/2).T(color)
    val SHOOT_CLOSE_LEAVE = Pose(20.0, 75.0, theta = -PI/2).T(color)
    val SHOOT_FAR = Pose(43.0, -140.0, theta = -PI/2).T(color)

    // Leave position
    val LEAVE_POSITION = Pose(60.0, -60.0).T(color)

    private fun collectY(i: Int) = 30 - (i * 60.0) // 30, -30, -90

    private val prepCollectX = 78.5
    private val collectTheta = -PI / 2 // Point to right
    val PREP_COLLECT_GOAL = Pose(prepCollectX, collectY(0), theta=collectTheta).T(color)
    val PREP_COLLECT_MID = Pose(prepCollectX, collectY(1), theta=collectTheta).T(color)
    val PREP_COLLECT_AUDIENCE = Pose(prepCollectX, collectY(2), theta=collectTheta).T(color)

    val PREP_COLLECT_START_VELOCITY = Pose(0.0, 0.0).T(color)
    val PREP_COLLECT_END_VELOCITY = Pose(80.0, 0.0).T(color)
    val GOTO_SHOOT_VELOCITY = Pose(-175.0, 0.0).T(color)

    private val collectX = 125.0
    val COLLECT_GOAL = Pose(collectX, collectY(0), theta=collectTheta).T(color)
    val COLLECT_MID = Pose(collectX, collectY(1), theta=collectTheta).T(color)
    val COLLECT_AUDIENCE = Pose(collectX, collectY(2), theta=collectTheta).T(color)

    // More complex paths
    fun PATH_HUMAN_PLAYER(startPose: Pose): Path {
        return HermitePath.Builder()
            .addPoint(startPose, Pose(0.0, 100.0).T(color))
            .addPoint(Pose(100.0, -110.0, theta = -8.0 * PI / 10.0).T(color))
            .addPoint(Pose(150.0, -150.0, theta = -9.0 * PI / 10.0).T(color), Pose(-40.0, -300.0).T(color))
            .build()
            .apply {
                // Slow down near the end of the path
                velocityConstraint = VelocityConstraint { s ->
                    if (s > this.getLength() - 37.5) 22.5 else Double.MAX_VALUE
                }
            }
    }

    fun PATH_COLLECT_AUDIENCE(startPose: Pose) : Path {
        return CompoundPath.Builder()
            .addPath(
                HermitePath.Builder()
                    .addPoint(startPose, Pose(0.0, 100.0).T(color))
                    .addPoint(Pose(60.0, -95.0, theta = -PI/2).T(color), Pose(100.0, 0.0).T(color))
                    .build()
            )
            .addPath(
                LinearPath.Builder()
                    .addPoint(Pose(60.0, -95.0, theta = -PI/2).T(color))
                    .addPoint(Pose(127.5, -90.0, theta = -PI/2).T(color))
                    .build()
            )
            .build().apply {
                // Slow down for collection
                velocityConstraint = VelocityConstraint { s ->
                    if (s > this.getLength() - 55.0) 10.0 else Double.MAX_VALUE
                }
            }
    }

    fun PATH_COLLECT_MID(startPose: Pose) : Path {
        return CompoundPath.Builder()
            .addPath(
                HermitePath.Builder()
                    .addPoint(startPose, Pose(0.0, 100.0).T(color))
                    .addPoint(Pose(78.5, -30.0, theta = -PI/2).T(color), Pose(100.0, 0.0).T(color))
                    .build()
            )
            .addPath(
                LinearPath.Builder()
                    .addPoint(Pose(78.5, -30.0, theta = -PI/2).T(color))
                    .addPoint(Pose(127.5, -30.0, theta = -PI/2).T(color))
                    .build()
            )
            .build().apply {
                // Slow down for collection
                velocityConstraint = VelocityConstraint { s ->
                    if (s > this.getLength() - 45.0) 10.0 else Double.MAX_VALUE
                }
            }
    }
}
