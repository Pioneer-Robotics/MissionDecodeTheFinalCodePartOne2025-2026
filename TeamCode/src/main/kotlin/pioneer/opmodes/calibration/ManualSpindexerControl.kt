package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotorEx
import pioneer.Constants

//@Disabled
@TeleOp(name = "Manual Spindexer Control", group = "Calibration")
class ManualSpindexerControl : OpMode() {
    private lateinit var motor: DcMotorEx

    var power = 0.0

    override fun init() {
        motor = hardwareMap.get(DcMotorEx::class.java, Constants.HardwareNames.SPINDEXER_MOTOR)
    }

    override fun loop() {
//        if (gamepad1.right_trigger > 0.05) {
//            motor.power = gamepad1.right_trigger.toDouble()
//        } else if (gamepad1.left_trigger > 0.05) {
//            motor.power = -gamepad1.left_trigger.toDouble()
//        } else {
//            motor.power = 0.0
//        }

        if (gamepad1.right_bumper) power += 0.004
        if (gamepad1.left_bumper) power -= 0.004

        motor.power = power

        telemetry.addData("Motor Power", gamepad1.right_trigger)
        telemetry.addData("Motor Position", motor.currentPosition)
        telemetry.addData("Motor Velocity", motor.velocity)
        telemetry.update()
    }
}
