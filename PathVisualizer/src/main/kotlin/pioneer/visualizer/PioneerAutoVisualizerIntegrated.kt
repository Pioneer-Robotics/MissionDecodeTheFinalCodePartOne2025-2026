package pioneer.visualizer

import pioneer.helpers.Pose
import pioneer.pathing.paths.*
import pioneer.decode.Points
import pioneer.general.AllianceColor
import javax.swing.SwingUtilities

/**
 * PIONEER ROBOTICS - AUTONOMOUS PATH VISUALIZER
 * With CORRECT coordinate transformation!
 *
 * Your robot coordinate system:
 *   X: -130 to +130 (260 units, centered at 0)
 *   Y: -157 to +137 (294 units, offset -10 from center)
 *
 * Visualizer coordinate system:
 *   X: 0 to 144 (origin at front-left, audience side)
 *   Y: 0 to 144
 *
 * Transformation:
 *   viz_x = (robot_x + 130) * 0.5538
 *   viz_y = (robot_y + 157) * 0.4898
 */

class PioneerAutoVisualizerIntegrated : EnhancedPioneerPathVisualizer() {

    init {
        title = "Pioneer Robotics - DECODE Auto Visualizer (Team 327)"
    }

    // Constants for coordinate transformation
    // Individual padding for each side (adjust these as needed)
    private val PADDING_LEFT = 12.0    // Left wall (X min)
    private val PADDING_RIGHT = 12.0   // Right wall (X max)
    private val PADDING_AUDIENCE = 6.0 // Audience side (Y min)
    private val PADDING_GOAL = 18.0    // Goal side (Y max)

    private val PLAYABLE_WIDTH = 144.0 - PADDING_LEFT - PADDING_RIGHT   // Width after padding
    private val PLAYABLE_HEIGHT = 144.0 - PADDING_AUDIENCE - PADDING_GOAL  // Height after padding

    private val SCALE_X = PLAYABLE_WIDTH / 260.0   // Scale to fit in playable width
    private val SCALE_Y = PLAYABLE_HEIGHT / 294.0  // Scale to fit in playable height
    private val OFFSET_X = 130.0  // Add this before scaling
    private val OFFSET_Y = 157.0  // Add this before scaling

    // Transform from robot coordinates to visualizer coordinates
    private fun Pose.toViz(): Pose {
        return Pose(
            x = (this.x + OFFSET_X) * SCALE_X + PADDING_LEFT,
            y = (this.y + OFFSET_Y) * SCALE_Y + PADDING_AUDIENCE,
            theta = this.theta,
            vx = this.vx * SCALE_X,
            vy = this.vy * SCALE_Y,
            ax = this.ax * SCALE_X,
            ay = this.ay * SCALE_Y,
            omega = this.omega,
            alpha = this.alpha
        )
    }

    override fun getAvailablePaths(): Array<String> {
        return arrayOf(
            // Test paths
            "Test: Simple Line",
            "Test: Square Path",
            "Test: Diagonal",

            // AUDIENCE SIDE AUTO (Far Start)
            "Red: Audience Side - To Shoot",
            "Blue: Audience Side - To Shoot",
            "Red: Audience Side - To Collect",
            "Blue: Audience Side - To Collect",
            "Red: Audience Side FULL",
            "Blue: Audience Side FULL",

            // GOAL SIDE AUTO (Close Start)
            "Red: Goal Side - To Shoot",
            "Blue: Goal Side - To Shoot",
            "Red: Goal Side - Collect Goal Row",
            "Blue: Goal Side - Collect Goal Row",
            "Red: Goal Side - Collect Mid Row",
            "Blue: Goal Side - Collect Mid Row",
            "Red: Goal Side - Collect Audience Row",
            "Blue: Goal Side - Collect Audience Row",
            "Red: Goal Side FULL (All Cycles)",
            "Blue: Goal Side FULL (All Cycles)",
        )
    }

    override fun createPathFromName(name: String): Path? {
        return when (name) {
            // Test paths (already in visualizer coordinates)
            "Test: Simple Line" -> LinearPath(Pose(24.0, 24.0), Pose(120.0, 120.0))
            "Test: Square Path" -> CompoundPath(listOf(
                LinearPath(Pose(36.0, 36.0), Pose(108.0, 36.0)),
                LinearPath(Pose(108.0, 36.0), Pose(108.0, 108.0)),
                LinearPath(Pose(108.0, 108.0), Pose(36.0, 108.0)),
                LinearPath(Pose(36.0, 108.0), Pose(36.0, 36.0))
            ))
            "Test: Diagonal" -> LinearPath(Pose(12.0, 12.0), Pose(132.0, 132.0))

            // ========== AUDIENCE SIDE AUTO ==========
            "Red: Audience Side - To Shoot" -> {
                val P = Points(AllianceColor.RED)
                LinearPath(P.START_FAR.toViz(), P.SHOOT_GOAL_FAR.toViz())
            }
            "Red: Audience Side - To Collect" -> {
                val P = Points(AllianceColor.RED)
                LinearPath(P.SHOOT_GOAL_FAR.toViz(), P.PREP_COLLECT_AUDIENCE.toViz())
            }
            "Red: Audience Side FULL" -> {
                val P = Points(AllianceColor.RED)
                CompoundPath(listOf(
                    LinearPath(P.START_FAR.toViz(), P.SHOOT_GOAL_FAR.toViz()),
                    LinearPath(P.SHOOT_GOAL_FAR.toViz(), P.PREP_COLLECT_AUDIENCE.toViz())
                ))
            }

            "Blue: Audience Side - To Shoot" -> {
                val P = Points(AllianceColor.BLUE)
                LinearPath(P.START_FAR.toViz(), P.SHOOT_GOAL_FAR.toViz())
            }
            "Blue: Audience Side - To Collect" -> {
                val P = Points(AllianceColor.BLUE)
                LinearPath(P.SHOOT_GOAL_FAR.toViz(), P.PREP_COLLECT_AUDIENCE.toViz())
            }
            "Blue: Audience Side FULL" -> {
                val P = Points(AllianceColor.BLUE)
                CompoundPath(listOf(
                    LinearPath(P.START_FAR.toViz(), P.SHOOT_GOAL_FAR.toViz()),
                    LinearPath(P.SHOOT_GOAL_FAR.toViz(), P.PREP_COLLECT_AUDIENCE.toViz())
                ))
            }

            // ========== GOAL SIDE AUTO ==========
            "Red: Goal Side - To Shoot" -> {
                val P = Points(AllianceColor.RED)
                LinearPath(P.START_GOAL.copy(theta = 0.1).toViz(), P.SHOOT_GOAL_CLOSE.toViz())
            }
            "Red: Goal Side - Collect Goal Row" -> {
                val P = Points(AllianceColor.RED)
                CompoundPath(listOf(
                    LinearPath(P.SHOOT_GOAL_CLOSE.toViz(), P.PREP_COLLECT_GOAL.toViz()),
                    LinearPath(P.PREP_COLLECT_GOAL.toViz(), P.COLLECT_GOAL.toViz())
                ))
            }
            "Red: Goal Side - Collect Mid Row" -> {
                val P = Points(AllianceColor.RED)
                CompoundPath(listOf(
                    LinearPath(P.COLLECT_GOAL.toViz(), P.SHOOT_GOAL_CLOSE.toViz()),
                    LinearPath(P.SHOOT_GOAL_CLOSE.toViz(), P.PREP_COLLECT_MID.toViz()),
                    LinearPath(P.PREP_COLLECT_MID.toViz(), P.COLLECT_MID.toViz())
                ))
            }
            "Red: Goal Side - Collect Audience Row" -> {
                val P = Points(AllianceColor.RED)
                CompoundPath(listOf(
                    LinearPath(P.COLLECT_MID.toViz(), P.SHOOT_GOAL_CLOSE.toViz()),
                    LinearPath(P.SHOOT_GOAL_CLOSE.toViz(), P.PREP_COLLECT_AUDIENCE.toViz()),
                    LinearPath(P.PREP_COLLECT_AUDIENCE.toViz(), P.COLLECT_AUDIENCE.toViz())
                ))
            }
            "Red: Goal Side FULL (All Cycles)" -> {
                val P = Points(AllianceColor.RED)
                CompoundPath(listOf(
                    LinearPath(P.START_GOAL.copy(theta = 0.1).toViz(), P.SHOOT_GOAL_CLOSE.toViz()),
                    LinearPath(P.SHOOT_GOAL_CLOSE.toViz(), P.PREP_COLLECT_GOAL.toViz()),
                    LinearPath(P.PREP_COLLECT_GOAL.toViz(), P.COLLECT_GOAL.toViz()),
                    LinearPath(P.COLLECT_GOAL.toViz(), P.SHOOT_GOAL_CLOSE.toViz()),
                    LinearPath(P.SHOOT_GOAL_CLOSE.toViz(), P.PREP_COLLECT_MID.toViz()),
                    LinearPath(P.PREP_COLLECT_MID.toViz(), P.COLLECT_MID.toViz()),
                    LinearPath(P.COLLECT_MID.toViz(), P.SHOOT_GOAL_CLOSE.toViz()),
                    LinearPath(P.SHOOT_GOAL_CLOSE.toViz(), P.PREP_COLLECT_AUDIENCE.toViz()),
                    LinearPath(P.PREP_COLLECT_AUDIENCE.toViz(), P.COLLECT_AUDIENCE.toViz()),
                    LinearPath(P.COLLECT_AUDIENCE.toViz(), P.SHOOT_GOAL_CLOSE.toViz())
                ))
            }

            "Blue: Goal Side - To Shoot" -> {
                val P = Points(AllianceColor.BLUE)
                LinearPath(P.START_GOAL.copy(theta = 0.1).toViz(), P.SHOOT_GOAL_CLOSE.toViz())
            }
            "Blue: Goal Side - Collect Goal Row" -> {
                val P = Points(AllianceColor.BLUE)
                CompoundPath(listOf(
                    LinearPath(P.SHOOT_GOAL_CLOSE.toViz(), P.PREP_COLLECT_GOAL.toViz()),
                    LinearPath(P.PREP_COLLECT_GOAL.toViz(), P.COLLECT_GOAL.toViz())
                ))
            }
            "Blue: Goal Side - Collect Mid Row" -> {
                val P = Points(AllianceColor.BLUE)
                CompoundPath(listOf(
                    LinearPath(P.COLLECT_GOAL.toViz(), P.SHOOT_GOAL_CLOSE.toViz()),
                    LinearPath(P.SHOOT_GOAL_CLOSE.toViz(), P.PREP_COLLECT_MID.toViz()),
                    LinearPath(P.PREP_COLLECT_MID.toViz(), P.COLLECT_MID.toViz())
                ))
            }
            "Blue: Goal Side - Collect Audience Row" -> {
                val P = Points(AllianceColor.BLUE)
                CompoundPath(listOf(
                    LinearPath(P.COLLECT_MID.toViz(), P.SHOOT_GOAL_CLOSE.toViz()),
                    LinearPath(P.SHOOT_GOAL_CLOSE.toViz(), P.PREP_COLLECT_AUDIENCE.toViz()),
                    LinearPath(P.PREP_COLLECT_AUDIENCE.toViz(), P.COLLECT_AUDIENCE.toViz())
                ))
            }
            "Blue: Goal Side FULL (All Cycles)" -> {
                val P = Points(AllianceColor.BLUE)
                CompoundPath(listOf(
                    LinearPath(P.START_GOAL.copy(theta = 0.1).toViz(), P.SHOOT_GOAL_CLOSE.toViz()),
                    LinearPath(P.SHOOT_GOAL_CLOSE.toViz(), P.PREP_COLLECT_GOAL.toViz()),
                    LinearPath(P.PREP_COLLECT_GOAL.toViz(), P.COLLECT_GOAL.toViz()),
                    LinearPath(P.COLLECT_GOAL.toViz(), P.SHOOT_GOAL_CLOSE.toViz()),
                    LinearPath(P.SHOOT_GOAL_CLOSE.toViz(), P.PREP_COLLECT_MID.toViz()),
                    LinearPath(P.PREP_COLLECT_MID.toViz(), P.COLLECT_MID.toViz()),
                    LinearPath(P.COLLECT_MID.toViz(), P.SHOOT_GOAL_CLOSE.toViz()),
                    LinearPath(P.SHOOT_GOAL_CLOSE.toViz(), P.PREP_COLLECT_AUDIENCE.toViz()),
                    LinearPath(P.PREP_COLLECT_AUDIENCE.toViz(), P.COLLECT_AUDIENCE.toViz()),
                    LinearPath(P.COLLECT_AUDIENCE.toViz(), P.SHOOT_GOAL_CLOSE.toViz())
                ))
            }

            else -> null
        }
    }
}

fun main() {
    SwingUtilities.invokeLater {
        PioneerAutoVisualizerIntegrated()
    }
}