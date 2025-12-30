package pioneer.opmodes.calibration

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.hardware.Spindexer
import pioneer.helpers.Chrono
import pioneer.helpers.Toggle
import pioneer.helpers.next
import kotlin.time.DurationUnit

@Disabled
@TeleOp(name = "Spindexer Motor Control", group = "Calibration")
class SpindexerMotorControl : OpMode() {
    lateinit var spindexer: Spindexer

    val changePositionToggle = Toggle(false)
    val applyPositionToggle = Toggle(false)
    val chrono = Chrono(autoUpdate = true, units = DurationUnit.MILLISECONDS)

    var targetPosition = Spindexer.MotorPosition.INTAKE_1

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
            spindexer.motorState = targetPosition
        }

        spindexer.update(chrono.dt)

        telemetry.addData("Target Position", targetPosition)
        telemetry.update()

        FtcDashboard.getInstance().telemetry.addData("Motor position", spindexer.currentMotorTicks)
        FtcDashboard.getInstance().telemetry.addData("Target position", spindexer.targetMotorTicks)
        FtcDashboard.getInstance().telemetry.update()
    }
}
