package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.Servo

@Disabled
@TeleOp(name = "Servo Calibration", group = "Calibration")
class ServoCalibration : OpMode() {
    lateinit var servo: Servo

    override fun init() {
        servo = hardwareMap.get(Servo::class.java, "launchServo")
    }

    override fun loop() {
        servo.position = if (gamepad1.right_bumper) 0.067 else 0.3

        servo.position = gamepad1.right_trigger.toDouble() * 0.2

        telemetry.addData("Servo Pos", servo.position)
        telemetry.update()
    }
}
