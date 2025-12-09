package pioneer.opmodes.other

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Bot
import pioneer.decode.Obelisk
import pioneer.general.AllianceColor
import pioneer.opmodes.BaseOpMode
import pioneer.vision.AprilTag

@Disabled
@TeleOp(name = "Obelisk Test")
class ObeliskTest : BaseOpMode() {
    private var alliance = AllianceColor.BLUE

    private val processor = AprilTag(draw = true).processor

    override fun onInit() {
        bot = Bot.builder().build()

        telemetry.addData("Instructions", "D-pad Up=Blue, Down=Red")
        telemetry.addData("Alliance", alliance)
        telemetry.update()
    }

    override fun onLoop() {
        // Toggle alliance color
        if (gamepad1.dpad_up) alliance = AllianceColor.BLUE
        if (gamepad1.dpad_down) alliance = AllianceColor.RED

        telemetry.addData("Alliance", alliance)
        telemetry.addLine()

        displayAllDetections()
        telemetry.addLine()
        findMotif()
    }

    private fun displayAllDetections() {
        val detections = processor.detections
        telemetry.addData("Tags Detected", detections.size)

        if (detections.isEmpty()) {
            telemetry.addData("Status", "No AprilTags visible")
            return
        }

        detections.forEachIndexed { index, detection ->
            telemetry.addLine("--- Tag ${index + 1} ---")
            telemetry.addData("  ID", detection.id)
            telemetry.addData("  Margin", "%.2f".format(detection.decisionMargin))

            detection.ftcPose?.let { pose ->
                telemetry.addData("  Position", "(%.1f, %.1f, %.1f)".format(pose.x, pose.y, pose.z))
                telemetry.addData("  Range", "%.1f cm".format(pose.range))
                telemetry.addData("  Bearing", "%.1f°".format(Math.toDegrees(pose.bearing)))
                telemetry.addData("  Elevation", "%.1f°".format(Math.toDegrees(pose.elevation)))
            } ?: telemetry.addData("  Pose", "Not calibrated")

            detection.metadata?.let {
                telemetry.addData("  Name", it.name)
            }
        }
    }

    private fun findMotif() {
        val detections = processor.detections
        val motif = Obelisk.detectMotif(detections, alliance)

        if (motif != null && motif.isValid()) {
            telemetry.addData("Motif Tag", motif.aprilTagId)
            telemetry.addData("Current Artifact", motif.currentArtifact?.toString() ?: "None")
            telemetry.addData("Position", "${motif.getCurrentIndex() + 1} of 3")
            telemetry.addData("Pattern", getPatternString(motif))
        } else {
            telemetry.addData("Motif", "Not detected")
            telemetry.addData("Look for", "Tags 21, 22, or 23")
        }
    }

    private fun getPatternString(motif: pioneer.decode.Motif): String {
        val pattern = motif.getPattern() ?: return "Unknown"
        return pattern.joinToString(" → ") { it.toString() }
    }
}
