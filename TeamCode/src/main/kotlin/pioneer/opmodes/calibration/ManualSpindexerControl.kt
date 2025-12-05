package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotorEx

@TeleOp(name = "Manual Spindexer Control", group = "Calibration")
class ManualSpindexerControl : OpMode() {
    private lateinit var motor: DcMotorEx

    override fun init() {
        motor = hardwareMap.get(DcMotorEx::class.java, "turretMotor")
    }

    override fun loop() {
        if (gamepad1.right_trigger > 0.05) {
            motor.power = gamepad1.right_trigger.toDouble()
        }
        if (gamepad1.left_trigger > 0.05) {
            motor.power = -gamepad1.left_trigger.toDouble()
        }

        telemetry.addData("Motor Position", motor.currentPosition)
        telemetry.update()
    }
}
