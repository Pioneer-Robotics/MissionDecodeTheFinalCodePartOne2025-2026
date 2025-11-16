package pioneer.decode

import org.firstinspires.ftc.robotcore.external.matrices.VectorF
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase
import pioneer.general.AllianceColor
import pioneer.helpers.MathUtils
import pioneer.helpers.Pose

private val tagLibrary by lazy { AprilTagGameDatabase.getDecodeTagLibrary() }

/**
 * Interface representing metadata for a Decode season goal AprilTag.
 */
interface GoalTagMetadata {
    val name: String
    val id: Int
    val alliance: AllianceColor
    val pose: Pose
    val height: Double
    val shootingOffset: Pose
}

/**
 * Abstract base class for FTC Decode season goal AprilTags.
 * Automatically fetches tag data from the AprilTag game database.
 */
sealed class GoalTag(
    override val id: Int,
    override val alliance: AllianceColor,
) : GoalTagMetadata {
    override val name: String
        get() = tagLibrary.lookupTag(id).name

    private val position: VectorF by lazy {
        tagLibrary.lookupTag(id).fieldPosition
    }

    private val orientation: Quaternion by lazy {
        tagLibrary.lookupTag(id).fieldOrientation
    }

    override val pose: Pose by lazy {
        Pose(
            x = MathUtils.inToCM(position[1].toDouble()),
            y = MathUtils.inToCM(-position[0].toDouble()),
            vx = 0.0,
            vy = 0.0,
            ax = 0.0,
            ay = 0.0,
            theta = MathUtils.quaternionToEuler(orientation).yaw,
            omega = 0.0,
            alpha = 0.0,
        )
    }

    override val height: Double
        get() = position[2].toDouble()

    override val shootingOffset: Pose
        get() = Pose(y = 46.45 / 2) // Half the goal depth (46.45 cm)
}

/**
 * Processor for FTC Decode season goal AprilTags.
 * Identifies and retrieves goal tag metadata based on tag ID.
 * @property tagId The ID of the goal AprilTag to process.
 */
object BlueGoal : GoalTag(20, AllianceColor.BLUE)

object RedGoal : GoalTag(24, AllianceColor.RED)

class GoalTagProcessor(
    private val tagId: Int,
) {
    val tag: GoalTagMetadata?
        get() =
            when (tagId) {
                20 -> BlueGoal
                24 -> RedGoal
                else -> null
            }

    val shootingPose: Pose? =
        tag?.let {
            it.pose + it.shootingOffset
        }

    fun isValid(): Boolean = tag != null

    fun isAlliance(alliance: AllianceColor): Boolean {
        val goalTag = tag ?: return false
        return goalTag.alliance == alliance
    }
}
