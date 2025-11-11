package pioneer.opmodes.other

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.BotType
import pioneer.opmodes.BaseOpMode

@TeleOp(name = "April Tags Test")
class AprilTagsTest : BaseOpMode(BotType.GOBILDA_STARTER_BOT) {
    override fun onInit() {
    }

    override fun onLoop() {
        calculateAprilTag()
        addAprilTagTelemetryData()
    }

    private fun fieldPosition() {
        val detections = bot.aprilTagProcessor.detections
        //TODO: Avg position if given multiple tags?
        for (detection in detections) {
            var tagPosition = listOf(detection.metadata.fieldPosition[0], detection.metadata.fieldPosition[1], detection.metadata.fieldPosition[2])
            var fieldPositionWithTag = listOf((tagPosition[0]+detection.ftcPose.x).toFloat(), (tagPosition[1]+detection.ftcPose.y).toFloat(), (tagPosition[1]+detection.ftcPose.z).toFloat())

            telemetry.addLine("--Field Position From Tag (x, y, z): (%.2f, %.2f, %.2f)".format(fieldPositionWithTag[0], fieldPositionWithTag[1], fieldPositionWithTag[2]))
            telemetry.addLine("--Bot Position (x, y): (%.2f, %.2f)".format(bot.localizer.pose.x, bot.localizer.pose.y))

        }
    }
    @Deprecated("ts sucks just use the library")
    private fun calculateAprilTag() {
        val detections = bot.aprilTagProcessor.detections
        for (detection in detections) {
            if (detection != null && detection.ftcPose != null) {
                var rho = detection.ftcPose.range
                var phi = detection.ftcPose.elevation
                var theta = detection.ftcPose.bearing

                var height = Math.sin(phi) * rho
                var hypotenuseXY = height / Math.tan(phi)
                var dX = Math.sin(theta) * hypotenuseXY
                var dY = Math.cos(theta) * hypotenuseXY

                telemetry.addLine("--Calculated Rel (x, y, z): (%.2f, %.2f,%.2f)".format(dX, dY, height))
            }
        }
    }

    private fun addAprilTagTelemetryData() {
        val detections = bot.aprilTagProcessor.getDetections()
        for (detection in detections) {
            // Check if tag or its properties are null to avoid null pointer exceptions
            if (detection != null && detection.ftcPose != null) {
                telemetry.addData("Detection", detection.id)
                telemetry.addLine(
                    "--Rel (x, y, z): (%.2f, %.2f, %.2f)".format(
                        detection.ftcPose.x,
                        detection.ftcPose.y,
                        detection.ftcPose.z,
                    ),
                )
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
