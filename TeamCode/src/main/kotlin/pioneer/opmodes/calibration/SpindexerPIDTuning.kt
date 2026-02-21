package pioneer.opmodes.calibration

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Constants
import pioneer.hardware.Flywheel
import pioneer.hardware.Intake
import pioneer.hardware.spindexer.Spindexer
import pioneer.helpers.Toggle

@TeleOp(name = "Spindexer PID Tuner", group = "Calibration")
class SpindexerPIDTuning : OpMode() {
    lateinit var spindexer: Spindexer
    lateinit var flywheel: Flywheel
    lateinit var intake: Intake
    var targetIndex = 0
    private val flywheelToggle = Toggle(false)
    private val intakeToggle = Toggle(false)
    private var direction = 1

    override fun init() {
        spindexer =
            Spindexer(
                hardwareMap = hardwareMap,
            ).apply { init() }

        flywheel =
            Flywheel(
                hardwareMap = hardwareMap,
            ).apply { init() }

        intake =
            Intake(
                hardwareMap = hardwareMap,
            ).apply { init() }
    }

    override fun loop() {
        if (gamepad1.circleWasPressed()) targetIndex = (targetIndex + 1) % 3
        if (gamepad1.crossWasPressed()) spindexer.moveToIndex(targetIndex, direction)
        if (gamepad1.leftBumperWasPressed()) direction *= -1
        flywheelToggle.toggle(gamepad1.dpad_left)
        intakeToggle.toggle(gamepad1.dpad_down)

        if (flywheelToggle.state) {
            flywheel.velocity = 500.0
        } else {
            flywheel.velocity = 0.0
        }

        if (intakeToggle.state) {
            intake.forward()
        } else {
            intake.stop()
        }

        spindexer.update()
        intake.update()
        flywheel.update()

        telemetry.addData("Spindexer Target", targetIndex)
        telemetry.addData("Direction", direction)
        telemetry.addData("Spindexer Motor Position", spindexer.currentMotorTicks)
        telemetry.addData("Target Position", spindexer.targetMotorTicks)
        telemetry.addData("Reached Target", spindexer.reachedTarget)
        telemetry.update()

        FtcDashboard.getInstance().telemetry.apply {
            addData("Spindexer Target", targetIndex)
            addData("Spindexer Motor Position", spindexer.currentMotorTicks)
            addData("Target Position", spindexer.targetMotorTicks)
            addData("Reached Target", spindexer.reachedTarget)
            update()
        }
    }
}
