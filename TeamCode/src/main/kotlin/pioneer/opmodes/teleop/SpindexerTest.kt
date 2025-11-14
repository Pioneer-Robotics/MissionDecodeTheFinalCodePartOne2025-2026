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
                outakeSensorName = "outakeSensor"
            ))
            .build()
    }

    override fun onLoop() {
        moveSpindexerToggle.toggle(gamepad1.circle)
        if (moveSpindexerToggle.justChanged) {
            bot.spindexer!!.motorState = bot.spindexer!!.motorState.next()
        }

        bot.spindexer!!.update()

        telemetry.addData("Spindexer Position", bot.spindexer!!.motorState.toString())
        telemetry.addData("Motor Ticks", bot.spindexer!!.motor.currentPosition)
        telemetry.addData("Target Ticks", bot.spindexer!!.motor.targetPosition)
    }
}
