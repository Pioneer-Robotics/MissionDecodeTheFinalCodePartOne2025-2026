package pioneer.opmodes.other

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.vision.apriltag.AprilTagLibrary
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.Bot
import pioneer.vision.AprilTag
import pioneer.hardware.Camera
import pioneer.localization.localizers.Pinpoint
import pioneer.opmodes.BaseOpMode
import kotlin.math.*

@TeleOp(name = "April Tags Test")
class AprilTagsTest : BaseOpMode() {
    private val processor : AprilTagProcessor = AprilTag(draw=true).processor

    override fun onInit() {
        bot = Bot.builder()
            .add(Pinpoint(hardwareMap))
            .add(Camera(hardwareMap, processors = arrayOf(processor)))
            .build()
    }

    override fun onLoop() {
        addAprilTagTelemetryData()
        fieldPosition()
    }

    private fun fieldPosition() {
        val detections = processor.detections
        //TODO: Avg position if given multiple tags?
        for (detection in detections) {
            val tagPosition = listOf(detection.metadata.fieldPosition.get(0), detection.metadata.fieldPosition.get(1), detection.metadata.fieldPosition.get(2))
            val fieldPositionWithTag = listOf((tagPosition[0]+detection.ftcPose.x), (tagPosition[1]+detection.ftcPose.y), (tagPosition[1]+detection.ftcPose.z))

            telemetry.addLine("--Field Position From Tag (x, y, z): (%.2f, %.2f, %.2f)".format(fieldPositionWithTag[0], fieldPositionWithTag[1], fieldPositionWithTag[2]))
            telemetry.addLine("--Tag Position (x, y, z): (%.2f, %.2f, %.2f)".format(tagPosition[0], tagPosition[1], tagPosition[2]))
            telemetry.addData("Test", detection.metadata.fieldPosition.data[0])

//            telemetry.addLine("--Bot Position (x, y): (%.2f, %.2f)".format(bot.pinpoint?.pose?.x, bot.pinpoint?.pose?.y))

        }
    }
    @Deprecated("ts sucks just use the library")
    private fun calculateAprilTag() {
        val detections = processor.detections
        for (detection in detections) {
            if (detection?.ftcPose != null) {
                val rho = detection.ftcPose.range
                val phi = detection.ftcPose.elevation
                val theta = detection.ftcPose.bearing

                val height = sin(phi) * rho
                val hypotenuseXY = height / tan(phi)
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
                    "--Rel (x, y, z): (%.2f, %.2f, %.2f)".format(
                        detection.ftcPose.x,
                        detection.ftcPose.y,
                        detection.ftcPose.z,
                    ),
                )
//                telemetry.addLine(
//                    "--Rel (Y, P, R): (%.2f, %.2f, %.2f)".format(
//                        detection.ftcPose.yaw,
//                        detection.ftcPose.pitch,
//                        detection.ftcPose.roll,
//                    ),
//                )
//                telemetry.addLine(
//                    "--Rel (R, B, E): (%.2f, %.2f, %.2f)".format(
//                        detection.ftcPose.range,
//                        detection.ftcPose.bearing,
//                        detection.ftcPose.elevation,
//                    ),
//                )
            } else {
                telemetry.addLine("No valid AprilTag detections.")
            }
        }
    }

}
