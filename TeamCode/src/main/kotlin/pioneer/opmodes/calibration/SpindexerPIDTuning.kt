package pioneer.opmodes.calibration

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.hardware.spindexer.Spindexer

@TeleOp(name = "Spindexer PID Tuner", group = "Calibration")
class SpindexerPIDTuning : OpMode() {
    lateinit var spindexer: Spindexer
    var targetIndex = 0

    override fun init() {
        spindexer =
            Spindexer(
                hardwareMap = hardwareMap,
            ).apply { init() }
    }

    override fun loop() {
        if (gamepad1.circleWasPressed()) targetIndex = (targetIndex + 1) % 3
        if (gamepad1.crossWasPressed()) spindexer.moveToIndex(targetIndex)

        spindexer.update()

        telemetry.addData("Spindexer Target", targetIndex)
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
