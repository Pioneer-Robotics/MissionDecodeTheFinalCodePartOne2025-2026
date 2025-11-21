package pioneer.opmodes.teleop

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Bot
import pioneer.decode.Artifact
import pioneer.hardware.Spindexer
import pioneer.opmodes.BaseOpMode

// Controls:
// D-Pad Down: Move spindexer to next open intake position
// Cross: Move spindexer to outake start position
// Circle: Move spindexer to outake next position

@TeleOp(name = "Spindexer Test")
class SpindexerTest : BaseOpMode() {

    override fun onInit() {
        bot = Bot.Builder()
            .add(Spindexer(
                hardwareMap = hardwareMap,
                motorName = "spindexerMotor",
                intakeSensorName = "intakeSensor",
                outakeSensorName = "outakeSensor",
            ))
            .build()
    }

    override fun onLoop() {
        if (gamepad1.dpad_down) bot.spindexer!!.moveToNextOpenIntake()
        if (gamepad1.left_bumper) bot.spindexer!!.moveToNextOutake(Artifact.GREEN)
        if (gamepad1.right_bumper) bot.spindexer!!.moveToNextOutake(Artifact.PURPLE)
        if (gamepad1.circle) bot.spindexer!!.consumeCurrentArtifact()

        bot.spindexer!!.update()

        telemetry.addData("Spindexer Position", bot.spindexer!!.motorState.toString())
        telemetry.addData("Artifacts", bot.spindexer!!.artifacts.contentDeepToString())
    }
}
