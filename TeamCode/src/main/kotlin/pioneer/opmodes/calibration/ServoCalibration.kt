package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.Servo
import pioneer.Constants
import pioneer.helpers.Toggle

@TeleOp(name = "Servo Calibration", group = "Calibration")
class ServoCalibration : OpMode() {
    lateinit var servo: Servo
    lateinit var motor: DcMotor
    val servoToggle = Toggle(false)

    override fun init() {
        servo = hardwareMap.get(Servo::class.java, Constants.HardwareNames.PTO_SERVO_L)
        motor = hardwareMap.get(DcMotor::class.java, Constants.HardwareNames.DRIVE_LEFT_FRONT)
    }

    override fun loop() {
        servoToggle.toggle(gamepad1.right_bumper)
        servo.position = if (servoToggle.state) 0.4 else 0.9

        motor.power = gamepad1.right_stick_y.toDouble()

//        if (gamepad1.right_trigger > 0.05) servo.position = gamepad1.right_trigger.toDouble()

        telemetry.addData("Servo Pos", servo.position)
        telemetry.update()
    }
}
