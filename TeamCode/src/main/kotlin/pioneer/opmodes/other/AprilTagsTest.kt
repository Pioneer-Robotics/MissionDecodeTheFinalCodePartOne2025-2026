package pioneer.opmodes.other

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Bot
import pioneer.BotType
import pioneer.opmodes.BaseOpMode

@TeleOp(name = "April Tags Test")
class AprilTagsTest : BaseOpMode(BotType.GOBILDA_STARTER_BOT) {

    override fun onInit(){

    }

    override fun onLoop() {
        calculateAprilTag()
        addAprilTagTelemetryData()

    }

    private fun calculateAprilTag(){
        val detections = bot.aprilTagProcessor.detections
        for (detection in detections){
            if (detection != null && detection.ftcPose != null) {
                var rho = detection.ftcPose.range
                var phi = detection.ftcPose.elevation
                var theta = detection.ftcPose.bearing

                var height = Math.sin(phi)*rho
                var hypotenuseXY = height/Math.tan(phi)
                var dX = Math.sin(theta)*hypotenuseXY
                var dY = Math.cos(theta)*hypotenuseXY

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
                        detection.ftcPose.z
                    )
                )
                telemetry.addLine(
                    "--Rel (Y, P, R): (%.2f, %.2f, %.2f)".format(
                        detection.ftcPose.yaw,
                        detection.ftcPose.pitch,
                        detection.ftcPose.roll
                    )
                )
                telemetry.addLine(
                    "--Rel (R, B, E): (%.2f, %.2f, %.2f)".format(
                        detection.ftcPose.range,
                        detection.ftcPose.bearing,
                        detection.ftcPose.elevation
                    )
                )
            } else {
                telemetry.addLine("No valid AprilTag detections.")
            }
        }
    }
}