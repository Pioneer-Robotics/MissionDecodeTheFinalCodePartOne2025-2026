package pioneer.opmodes.teleop

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Bot
import pioneer.Constants
import pioneer.decode.Motif
import pioneer.hardware.spindexer.Spindexer
import pioneer.opmodes.BaseOpMode

@TeleOp(name = "Spindexer Test")
class SpindexerTest : BaseOpMode() {
    var motifOrder: Motif? = Motif(22)
    var launchConditions = false
    var delayTimer = ElapsedTime()

    override fun onInit() {
        bot = Bot
                .Builder()
                .add(Spindexer(hardwareMap))
//                .add(Flywheel(hardwareMap))
//                .add(Intake(hardwareMap))
                .build()
    }

    override fun onLoop() {
        bot.updateAll()

        if (bot.spindexer!!.isShooting) {
            delayTimer.reset()
        }

        if (delayTimer.seconds() > Constants.Spindexer.SHOOT_ALL_DELAY) {
            launchConditions = true
        }

        if (gamepad1.dpad_down) bot.spindexer?.moveToNextOpenIntake()
        if (gamepad1.leftBumperWasPressed()) {
            motifOrder = motifOrder?.prevMotif()
            bot.spindexer?.readyOuttake(motifOrder)
        }
        if (gamepad1.rightBumperWasPressed()) {
            motifOrder = motifOrder?.nextMotif()
            bot.spindexer?.readyOuttake(motifOrder)
        }
        if (gamepad1.touchpadWasPressed()) bot.spindexer?.shootAll()
//        if (gamepad1.touchpadWasPressed()) bot.spindexer?.requestShootAll(launchConditions)
        if (gamepad1.circleWasPressed()) bot.spindexer?.shootNext()

        if (gamepad1.dpad_up) bot.intake?.forward() else bot.intake?.stop()
        if (gamepad1.dpad_right) bot.flywheel?.velocity = 600.0
        if (gamepad1.dpad_left) bot.flywheel?.velocity = 0.0

        telemetry.addData("Current Position", bot.spindexer?.currentMotorTicks)
        telemetry.addData("Target Position", bot.spindexer?.targetMotorTicks)
        telemetry.addData("Target Motif", motifOrder.toString())
        telemetry.addData("Artifacts", bot.spindexer?.artifacts.contentDeepToString())
        telemetry.addData("Reached Target", bot.spindexer?.reachedTarget)
        telemetry.addData("Shot Counter", bot.spindexer?.shotCounter)
        telemetry.addData("Ready for Next Shot", bot.spindexer?.readyForNextShot)
//        telemetry.addData("Just Finished Shot", bot.spindexer?.finishedShot)
        telemetry.addData("Shoot All Commanded", bot.spindexer?.shootAllCommanded)
//        telemetry.addData("Delay Timer", bot.spindexer?.delayTimer)
        telemetry.addData("Shot Counter", bot.spindexer?.shotCounter)
        telemetry.addData("Is Shooting", bot.spindexer?.isShooting)
    }
}

