package pioneer.opmodes.teleop

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Bot
import pioneer.decode.Motif
import pioneer.hardware.Flywheel
import pioneer.hardware.Intake
import pioneer.hardware.spindexer.Spindexer
import pioneer.opmodes.BaseOpMode

@TeleOp(name = "Spindexer Test")
class SpindexerTest : BaseOpMode() {
    var motifOrder: Motif? = Motif(22)

    override fun onInit() {
        bot = Bot
                .Builder()
                .add(Spindexer(hardwareMap))
                .add(Flywheel(hardwareMap))
                .add(Intake(hardwareMap))
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

        if (gamepad1.dpad_up) bot.intake?.forward() else bot.intake?.stop()
        if (gamepad1.dpad_right) bot.flywheel?.velocity = 600.0
        if (gamepad1.dpad_left) bot.flywheel?.velocity = 0.0

        telemetry.addData("Current Position", bot.spindexer?.currentMotorTicks)
        telemetry.addData("Target Position", bot.spindexer?.targetMotorTicks)
        telemetry.addData("Target Motif", motifOrder.toString())
        telemetry.addData("Artifacts", bot.spindexer?.artifacts.contentDeepToString())
        telemetry.addData("Reached Target", bot.spindexer?.reachedTarget)
    }
}
