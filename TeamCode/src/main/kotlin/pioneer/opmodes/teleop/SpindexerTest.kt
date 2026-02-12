package pioneer.opmodes.teleop

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Bot
import pioneer.decode.Motif
import pioneer.hardware.spindexer.Spindexer
import pioneer.opmodes.BaseOpMode

@TeleOp(name = "Spindexer Test")
class SpindexerTest : BaseOpMode() {
    var motifOrder: Motif? = Motif(21)

    override fun onInit() {
        bot = Bot
                .Builder()
                .add(Spindexer(hardwareMap))
                .build()
    }

    override fun onLoop() {
        if (gamepad1.dpad_down) bot.spindexer?.moveToNextOpenIntake()
        if (gamepad1.left_bumper) {
            motifOrder = motifOrder?.prevMotif()
            bot.spindexer?.readyOuttake(motifOrder)
        }
        if (gamepad1.left_bumper) {
            motifOrder = motifOrder?.nextMotif()
            bot.spindexer?.readyOuttake(motifOrder)
        }
        if (gamepad1.touchpad) bot.spindexer?.shootAll()
        if (gamepad1.circle) bot.spindexer?.shootNext()

        telemetry.addData("Current Position", bot.spindexer?.currentMotorTicks)
        telemetry.addData("Target Position", bot.spindexer?.targetMotorTicks)
        telemetry.addData("Spindexer Position", bot.spindexer?.motorState.toString())
        telemetry.addData("Artifacts", bot.spindexer?.artifacts.contentDeepToString())
        telemetry.addData("Reached Target", bot.spindexer?.reachedTarget)
    }
}
