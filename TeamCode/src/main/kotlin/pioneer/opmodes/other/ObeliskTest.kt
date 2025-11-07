package pioneer.opmodes.other

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.BotType
import pioneer.decode.Obelisk
import pioneer.general.AllianceColor
import pioneer.opmodes.BaseOpMode

@TeleOp(name = "ObeliskTest")
class ObeliskTest : BaseOpMode(BotType.GOBILDA_STARTER_BOT) {
    // Toggle alliance color with gamepad buttons
    private var alliance = AllianceColor.BLUE

    override fun onInit() {
        telemetry.addData("Instructions", "Use D-pad Up/Down to change alliance")
        telemetry.addData("Current Alliance", alliance)
        telemetry.update()
    }

    override fun onLoop() {
        // Allow toggling alliance color
        if (gamepad1.dpad_up) {
            alliance = AllianceColor.BLUE
        } else if (gamepad1.dpad_down) {
            alliance = AllianceColor.RED
        }

        telemetry.addData("Current Alliance", alliance)
        telemetry.addData("---", "---")

        // Display all AprilTag detections
        displayAllDetections()

        telemetry.addData("---", "---")

        // Find and display motif
        findMotif()
    }

    private fun displayAllDetections() {
        val detections = bot.aprilTagProcessor.detections

        telemetry.addData("Total Tags Detected", detections.size)

        if (detections.isEmpty()) {
            telemetry.addData("Status", "No AprilTags detected")
            return
        }

        // Display each detection with its position information
        detections.forEachIndexed { index, detection ->
            telemetry.addData("---", "Tag ${index + 1} ---")
            telemetry.addData("Tag ID", detection.id)
            telemetry.addData("  Decision Margin", "%.2f".format(detection.decisionMargin))

            // Display FTC Pose if available
            detection.ftcPose?.let { pose ->
                telemetry.addData(
                    "  Position (x,y,z)",
                    "%.1f, %.1f, %.1f".format(pose.x, pose.y, pose.z),
                )
                telemetry.addData(
                    "  Range/Bearing",
                    "%.1f / %.1f°".format(pose.range, Math.toDegrees(pose.bearing)),
                )
                telemetry.addData(
                    "  Elevation",
                    "%.1f°".format(Math.toDegrees(pose.elevation)),
                )
                telemetry.addData(
                    "  Yaw/Pitch/Roll",
                    "%.1f°, %.1f°, %.1f°".format(
                        Math.toDegrees(pose.yaw),
                        Math.toDegrees(pose.pitch),
                        Math.toDegrees(pose.roll),
                    ),
                )
            } ?: run {
                telemetry.addData("  FTC Pose", "Not available")
            }

            // Display raw pose if available
            detection.rawPose?.let { rawPose ->
                telemetry.addData(
                    "  Raw Position",
                    "%.2f, %.2f, %.2f".format(rawPose.x, rawPose.y, rawPose.z),
                )
            }

            // Display metadata
            detection.metadata?.let { metadata ->
                telemetry.addData("  Name", metadata.name)
                telemetry.addData("  Tag Size", "%.2f".format(metadata.tagsize))
            }
        }
    }

    private fun findMotif() {
        val detections = bot.aprilTagProcessor.detections
        val motif = Obelisk.detectMotif(detections, alliance)

        if (motif != null && motif.isValid()) {
            telemetry.addData("Motif Detected", "Tag ID ${motif.aprilTagId}")
            telemetry.addData("Pattern", getPatternString(motif))
            telemetry.addData(
                "Current Artifact",
                motif.currentArtifact()?.javaClass?.simpleName ?: "None",
            )
            telemetry.addData("Pattern Position", "${motif.getCurrentIndex() + 1} of 3")
        } else {
            telemetry.addData("Motif Detected", "None")
            telemetry.addData("Hint", "Look for tags 21, 22, or 23")
        }
    }

    private fun getPatternString(motif: pioneer.decode.Motif): String {
        val pattern = motif.getPattern() ?: return "Unknown"
        return pattern.joinToString(" → ") {
            when (it) {
                is pioneer.decode.GreenArtifact -> "Green"
                is pioneer.decode.PurpleArtifact -> "Purple"
                else -> "Unknown"
            }
        }
    }
}
