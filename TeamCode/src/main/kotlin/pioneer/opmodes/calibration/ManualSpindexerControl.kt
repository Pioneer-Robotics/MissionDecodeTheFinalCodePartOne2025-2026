package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotorEx
import pioneer.Constants
import pioneer.helpers.Chrono
import pioneer.helpers.PIDController

//@Disabled
@TeleOp(name = "Manual Spindexer Control", group = "Calibration")
class ManualSpindexerControl : OpMode() {
    private lateinit var motor: DcMotorEx

    var power = 0.0

    val chrono = Chrono()

    override fun init() {
        motor = hardwareMap.get(DcMotorEx::class.java, Constants.HardwareNames.INTAKE_MOTOR)
    }

    override fun loop() {
//        if (gamepad1.right_trigger > 0.05) {
//            motor.power = gamepad1.right_trigger.toDouble()
//        } else if (gamepad1.left_trigger > 0.05) {
//            motor.power = -gamepad1.left_trigger.toDouble()
//        } else {
//            motor.power = 0.0
//        }

        if (gamepad1.rightBumperWasPressed()) power += 0.05
        if (gamepad1.leftBumperWasPressed()) power -= 0.05

//        val targetVelocity = power * Constants.Spindexer.MAX_VELOCITY
//        motor.velocity = targetVelocity

        motor.power = power

        telemetry.addData("Motor Power", power)
        telemetry.addData("Motor Position", motor.currentPosition)
        telemetry.addData("Motor Velocity", motor.velocity)
        telemetry.update()
    }
}
