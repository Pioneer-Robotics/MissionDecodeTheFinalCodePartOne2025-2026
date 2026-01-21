package pioneer.opmodes.calibration

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.hardware.spindexer.Spindexer
import pioneer.hardware.spindexer.SpindexerMotionController
import pioneer.helpers.Toggle
import pioneer.helpers.next

//@Disabled
@TeleOp(name = "Spindexer Motor Control", group = "Calibration")
class SpindexerMotorControl : OpMode() {
    lateinit var spindexer: Spindexer

    val changePositionToggle = Toggle(false)
    val applyPositionToggle = Toggle(false)

    var targetPosition = SpindexerMotionController.MotorPosition.INTAKE_1

    override fun init() {
        spindexer = Spindexer(hardwareMap).apply { init() }
    }

    override fun loop() {
        changePositionToggle.toggle(gamepad1.circle)
        applyPositionToggle.toggle(gamepad1.cross)

        if (changePositionToggle.justChanged) {
            targetPosition = targetPosition.next()
        }

        if (applyPositionToggle.justChanged) {
            spindexer.moveToPosition(targetPosition)
        }

        spindexer.update()

        telemetry.addData("Target Position", targetPosition)
        telemetry.update()

        FtcDashboard.getInstance().telemetry.addData("Motor position", spindexer.currentMotorTicks)
        FtcDashboard.getInstance().telemetry.addData("Target position", spindexer.targetMotorTicks)
        FtcDashboard.getInstance().telemetry.update()
    }
}
