package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.Servo
import org.firstinspires.ftc.robotcore.external.Const
import pioneer.Constants

@TeleOp(name = "Servo Calibration", group = "Calibration")
class ServoCalibration : OpMode() {
    lateinit var servo: Servo
    var pos1 = 0.5
    var pos2 = 0.5
    var toggled = false

    override fun init() {
        servo = hardwareMap.get(Servo::class.java, Constants.HardwareNames.PTO_SERVO_R)
    }

    override fun loop() {
//        servo.position = if (gamepad1.right_bumper) 0.3 else 0.7

//        if (gamepad1.right_trigger > 0.05) servo.position = gamepad1.right_trigger.toDouble()
//        if (gamepad1.rightBumperWasPressed()) target += 0.05
//        if (gamepad1.leftBumperWasPressed()) target -= 0.05
//        target.coerceIn(0.0, 1.0)
//        servo.position = target

        if (gamepad1.circleWasPressed()) toggled = !toggled
        if (gamepad1.rightBumperWasPressed()) if (toggled) pos1 += 0.05 else pos2 += 0.05
        if (gamepad1.leftBumperWasPressed()) if (toggled) pos1 -= 0.05 else pos2 -= 0.05
        servo.position = if (toggled) pos1 else pos2

        pos1.coerceIn(0.0, 1.0)
        pos2.coerceIn(0.0, 1.0)

        telemetry.addData("Target", if (toggled) "pos1" else "pos2")
        telemetry.addData("Pos 1", pos1)
        telemetry.addData("Pos 2", pos2)
        telemetry.update()
    }
}
