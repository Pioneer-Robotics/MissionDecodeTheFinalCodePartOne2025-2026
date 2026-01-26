package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.Servo

@TeleOp(name = "Servo Calibration", group = "Calibration")
class ServoCalibration : OpMode() {
    lateinit var servo: Servo

    override fun init() {
        servo = hardwareMap.get(Servo::class.java, "launchServo")
    }

    override fun loop() {
        servo.position = if (gamepad1.right_bumper) 0.3 else 0.7

        if (gamepad1.right_trigger > 0.05) servo.position = gamepad1.right_trigger.toDouble()

        telemetry.addData("Servo Pos", servo.position)
        telemetry.update()
    }
}
