package pioneer.opmodes.teleop

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Bot
import pioneer.decode.Artifact
import pioneer.hardware.Spindexer
import pioneer.opmodes.BaseOpMode

// Controls:
// D-Pad Down: Move spindexer to next open intake position
// Cross: Move spindexer to outtake start position
// Circle: Move spindexer to outtake next position

@TeleOp(name = "Spindexer Test")
class SpindexerTest : BaseOpMode() {
    override fun onInit() {
        bot =
            Bot
                .Builder()
                .add(Spindexer(hardwareMap))
                .build()
    }

    override fun onLoop() {
        if (gamepad1.dpad_down) bot.spindexer!!.moveToNextOpenIntake()
        if (gamepad1.left_bumper) bot.spindexer!!.moveToNextOuttake(Artifact.GREEN)
        if (gamepad1.right_bumper) bot.spindexer!!.moveToNextOuttake(Artifact.PURPLE)
        if (gamepad1.touchpad) bot.spindexer!!.moveToNextOuttake()
        if (gamepad1.circle) bot.spindexer!!.popCurrentArtifact()

        bot.spindexer!!.update()

        telemetry.addData("Current Position", bot.spindexer!!.currentMotorPosition)
        telemetry.addData("Target Position", bot.spindexer!!.targetMotorPosition)
        telemetry.addData("Spindexer Position", bot.spindexer!!.motorState.toString())
        telemetry.addData("Artifacts", bot.spindexer!!.artifacts.contentDeepToString())
        telemetry.addData("Current Scanned Artifact", bot.spindexer!!.currentScannedArtifact)
        telemetry.addData("Reached Target", bot.spindexer!!.reachedTarget)
    }
}
