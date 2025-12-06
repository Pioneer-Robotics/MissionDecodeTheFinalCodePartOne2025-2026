package pioneer.opmodes.other

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.Bot
import pioneer.decode.GoalTag
import pioneer.decode.GoalTagProcessor
import pioneer.hardware.Camera
import pioneer.opmodes.BaseOpMode
import pioneer.vision.AprilTag
import kotlin.math.*

@Disabled
@TeleOp(name = "April Tags Test")
class AprilTagsTest : BaseOpMode() {
    private val processor: AprilTagProcessor = AprilTag(draw = true).processor

    override fun onInit() {
        bot =
            Bot
                .builder()
//            .add(Pinpoint(hardwareMap))
                .add(Camera(hardwareMap, processors = arrayOf(processor)))
                .build()
    }

    override fun onLoop() {
        addAprilTagTelemetryData()
        fieldPosition()
        calculateAprilTag()
    }

    private fun fieldPosition() {
        val detections = processor.detections
        // TODO: Avg position if given multiple tags?
//        val goalTagProcessorInstant = GoalTagProcessor()

        val tagInfo = GoalTagProcessor.getRobotFieldPose(detections)

//        val tagInfo = goalTagProcessorInstant.getRobotFieldPose()

        telemetry.addLine("--Field Position From Tag (x, y): (%.2f, %.2f)".format(tagInfo?.x, tagInfo?.y))
        telemetry.addData("Pose from Tag", tagInfo.toString())
        telemetry.addData("Tag Metadata", GoalTag.BLUE.pose)

//        telemetry.addLine("--Tag Position (x, y): (%.2f, %.2f, %.2f)".format(tagInfo?.x, tagPosition?.y))
//            telemetry.addLine("--Bot Position (x, y): (%.2f, %.2f)".format(bot.pinpoint?.pose?.x, bot.pinpoint?.pose?.y))
    }

//    @Deprecated("ts sucks just use the library")
    private fun calculateAprilTag() {
        val detections = processor.detections
        val tagInfo = GoalTagProcessor.getRobotFieldPose(detections)

        for (detection in detections) {
            if (detection?.ftcPose != null) {
                val tgRelPose = detection.ftcPose
                // Untested, but potential ways to calculate
                val rangeShadow = cos(tgRelPose.elevation) * tgRelPose.range
                // Method 1: Angle origin is on camera
                // yaw+theta gives robot theta, then - bearing?
                val insideAngle1 = (-tgRelPose.yaw + tagInfo!!.theta) - tgRelPose.bearing // Not sure about signs(+/-) here
                val dX1 = rangeShadow * sin(insideAngle1)
                val dY1 = rangeShadow * cos(insideAngle1)
                // Method 2: Angle origin is on AprilTag
                val insideAngle2 = (PI / 2 - tagInfo.theta) - tgRelPose.yaw - tgRelPose.bearing
                val dX2 = rangeShadow * cos(insideAngle2)
                val dy2 = rangeShadow * sin(insideAngle2)

                telemetry.addLine("--Calculated Rel M1(x, y): (%.2f, %.2f)".format(dX1, dY1))
                telemetry.addLine("--Calculated Rel M2(x, y): (%.2f, %.2f)".format(dX2, dy2))
            }
        }
    }

    private fun addAprilTagTelemetryData() {
        val detections = processor.detections
        for (detection in detections) {
            // Check if tag or its properties are null to avoid null pointer exceptions
            if (detection?.ftcPose != null) {
                telemetry.addData("Detection", detection.id)
//                telemetry.addLine(
//                    "--Rel FTC(x, y, z, yaw): (%.2f, %.2f, %.2f)".format(
//                        detection.ftcPose.x,
//                        detection.ftcPose.y,
//                        detection.ftcPose.z,
//                    ),
//                )
//                telemetry.addLine(
//                    "--Rel Rob Pose (x, y, z): (%.2f, %.2f, %.2f)".format(
//                        detection.robotPose.position.x,
//                        detection.robotPose.position.y,
//                        detection.robotPose.position.z,
//                    ),
//                )
                telemetry.addLine(
                    "--Rel (Y, P, R): (%.2f, %.2f, %.2f)".format(
                        detection.ftcPose.yaw,
                        detection.ftcPose.pitch,
                        detection.ftcPose.roll,
                    ),
                )
                telemetry.addLine(
                    "--Rel (R, B, E): (%.2f, %.2f, %.2f)".format(
                        detection.ftcPose.range,
                        detection.ftcPose.bearing,
                        detection.ftcPose.elevation,
                    ),
                )
            } else {
                telemetry.addLine("No valid AprilTag detections.")
            }
        }
    }
}
