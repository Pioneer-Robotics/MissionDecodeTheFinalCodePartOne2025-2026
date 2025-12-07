package pioneer.decode

import org.firstinspires.ftc.robotcore.external.matrices.VectorF
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase
import pioneer.general.AllianceColor
import pioneer.helpers.MathUtils
import pioneer.helpers.Pose
import kotlin.math.PI
import kotlin.math.sqrt

private val tagLibrary by lazy { AprilTagGameDatabase.getDecodeTagLibrary() }

/**
 * Enum representing metadata for Decode season goal AprilTags.
 */
enum class GoalTag(
    val id: Int,
    val alliance: AllianceColor,
) {
    BLUE(20, AllianceColor.BLUE),
    RED(24, AllianceColor.RED),
    ;

    val tagName: String // Retrieves the tag-specific name
        get() = tagLibrary.lookupTag(id).name

    private val position: VectorF by lazy {
        tagLibrary.lookupTag(id).fieldPosition
    }

    private val orientation: Quaternion by lazy {
        tagLibrary.lookupTag(id).fieldOrientation
    }

    val pose: Pose by lazy {
        Pose(
            x = MathUtils.inToCM(position[1].toDouble()),
            y = MathUtils.inToCM(-position[0].toDouble()),
            theta = MathUtils.quaternionToEuler(orientation).yaw - Math.PI,
        )
    }

    val height: Double
        get() = position[2].toDouble()

    // Tag pose plus half the goal depth (46.45 cm)
    val shootingPose: Pose
        get() =
            when (this) {
                BLUE -> this.pose + Pose(x = -(46.45 / 2) / sqrt(2.0), y = (46.45 / 2) / sqrt(2.0)) // -X +Y
                RED -> this.pose + Pose(x = (46.45 / 2) / sqrt(2.0), y = (46.45 / 2) / sqrt(2.0)) // +X +Y
            }
}

/**
 * Processor for FTC Decode season goal AprilTags.
 * Identifies and retrieves goal tag metadata based on tag ID.
 * @property tagId The ID of the goal AprilTag to process.
 */
object GoalTagProcessor {
    private val validTags = GoalTag.values().map { it.id }.toSet()

    fun isValidGoalTag(aprilTagId: Int): Boolean = aprilTagId in validTags

    // Detects the goal tag based on alliance color and returns the corresponding GoalTag object.
    fun getGoalTag(
        detections: List<AprilTagDetection>,
        alliance: AllianceColor,
    ): GoalTag? {
        val validDetections = detections.filter { it.ftcPose != null && isValidGoalTag(it.id) }
        return when (alliance) {
            AllianceColor.BLUE -> {
                validDetections
                    .maxByOrNull { it.ftcPose.x }
                    ?.takeIf { it.id == GoalTag.BLUE.id }
                    ?.let { GoalTag.BLUE }
            }

            AllianceColor.RED -> {
                validDetections
                    .minByOrNull { it.ftcPose.x }
                    ?.takeIf { it.id == GoalTag.RED.id }
                    ?.let { GoalTag.RED }
            }

            AllianceColor.NEUTRAL -> {
                null
            }
        }
    }

    // Computes the robot's field pose based on detected goal tags.
    fun getRobotFieldPose(detections: List<AprilTagDetection>): Pose? {
        val tag =
            detections.firstNotNullOfOrNull { detection ->
                GoalTag.values().firstOrNull { it.id == detection.id }
            }

        return tag?.pose?.let { tagPose ->
            detections.firstNotNullOfOrNull { detection ->
                when (detection.id) {
                    GoalTag.BLUE.id, GoalTag.RED.id -> {
                        detection.ftcPose?.let { ftcPose ->
                            Pose(
                                x = tagPose.x + MathUtils.inToCM(ftcPose.x.toDouble()),
                                y = tagPose.y + MathUtils.inToCM(ftcPose.y.toDouble()),
                                theta = tagPose.theta + ftcPose.bearing,
                            )
                        }
                    }

                    else -> {
                        null
                    }
                }
            }
        }
    }
}

// X and Y distance from center of camera to AprilTag
//    fun getRobotDistance(detections: List<AprilTagDetection>): List<Double>?{
//        val tag =
//            detections.firstNotNullOfOrNull { detection ->
//                GoalTag.values().firstOrNull { it.id == detection.id }
//            }
//
//        detections.firstNotNullOfOrNull { detection ->
//            when (detection.id){
//                GoalTag.BLUE.id, GoalTag.RED.id -> {
//                    detection.ftcPose?.let { ftcPose ->
//
//                    }
//                    val rangeShadow = detection.ftcPose.range* cos(detection.ftcPose.elevation)
//                    var theta: Double
//
//
//                }
//                }
//            }
//        }
//
//    }

// Not sure if needed
//    fun getTagData(detections: List<AprilTagDetection>): Pose? {
//        val tag = detections.firstNotNullOfOrNull {detection ->
//            when (detection.id) {
//                20 -> BlueGoal
//                24 -> RedGoal
//                else -> null
//            }
//        }
//        return tag?.pose
//    }
// }
