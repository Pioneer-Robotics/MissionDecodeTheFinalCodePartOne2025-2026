package pioneer.opmodes.teleop

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotor
import pioneer.Bot
import pioneer.hardware.Spindexer
import pioneer.helpers.Toggle
import pioneer.helpers.next
import pioneer.opmodes.BaseOpMode
import kotlin.enums.enumEntries

@TeleOp(name = "Spindexer Test")
class SpindexerTest : BaseOpMode() {

    private val moveSpindexerToggle = Toggle(false)

    override fun onInit() {
        bot = Bot.Builder()
            .add(Spindexer(
                hardwareMap = hardwareMap,
                motorName = "spindexerMotor",
                intakeSensorName = "intakeSensor",
                outakeSensorName = "outakeSensor",
                telemetry = telemetry
            ))
            .build()
    }

    override fun onLoop() {
        if (gamepad1.dpad_down) bot.spindexer!!.moveToNext()
        if (gamepad1.cross) bot.spindexer!!.moveToOutakeStart()

        bot.spindexer!!.update()

        telemetry.addData("Spindexer Position", bot.spindexer!!.motorState.toString())
        telemetry.addData("Motif", bot.spindexer!!.motif.toString())
        telemetry.addData("Artifacts", bot.spindexer!!.artifacts.contentDeepToString())
    }
}
