package pioneer.opmodes.other

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.Bot
import pioneer.decode.BlueGoal
import pioneer.decode.GoalTagProcessor
import pioneer.vision.AprilTag
import pioneer.hardware.Camera
import pioneer.opmodes.BaseOpMode
import kotlin.math.*

@TeleOp(name = "April Tags Test")
class AprilTagsTest : BaseOpMode() {
    private val processor : AprilTagProcessor = AprilTag(draw=true).processor

    override fun onInit() {
        bot = Bot.builder()
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
        //TODO: Avg position if given multiple tags?
//        val goalTagProcessorInstant = GoalTagProcessor()

        val tagInfo = GoalTagProcessor().getRobotFieldPose(detections)

//        val tagInfo = goalTagProcessorInstant.getRobotFieldPose()

        telemetry.addLine("--Field Position From Tag (x, y): (%.2f, %.2f)".format(tagInfo?.x, tagInfo?.y))
        telemetry.addData("Pose from Tag", tagInfo.toString())
        telemetry.addData("Tag Metadata", BlueGoal.pose)

//        telemetry.addLine("--Tag Position (x, y): (%.2f, %.2f, %.2f)".format(tagInfo?.x, tagPosition?.y))
//            telemetry.addLine("--Bot Position (x, y): (%.2f, %.2f)".format(bot.pinpoint?.pose?.x, bot.pinpoint?.pose?.y))

    }
//    @Deprecated("ts sucks just use the library")
    private fun calculateAprilTag() {
        val detections = processor.detections
        for (detection in detections) {
            if (detection?.ftcPose != null) {
                val rho = detection.ftcPose.range
                val phi = detection.ftcPose.elevation
                val theta = detection.ftcPose.bearing

                val height = sin(phi) * rho
                val hypotenuseXY = cos(phi) * rho
                val dX = sin(theta) * hypotenuseXY
                val dY = cos(theta) * hypotenuseXY

                telemetry.addLine("--Calculated Rel (x, y, z): (%.2f, %.2f,%.2f)".format(dX, dY, height))
            }
        }
    }

    private fun addAprilTagTelemetryData() {
        val detections = processor.detections
        for (detection in detections) {
            // Check if tag or its properties are null to avoid null pointer exceptions
            if (detection?.ftcPose != null) {
                telemetry.addData("Detection", detection.id)
                telemetry.addLine(
                    "--Rel (x, y, z, yaw): (%.2f, %.2f, %.2f)".format(
                        detection.ftcPose.x,
                        detection.ftcPose.y,
                        detection.ftcPose.z,
                    ),
                )
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
