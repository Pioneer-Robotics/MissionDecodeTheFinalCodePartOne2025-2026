package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.hardware.MecanumBase

@TeleOp(name = "Individual Motor Control", group = "Calibration")
class IndividualMotorControl : OpMode() {
    private lateinit var base: MecanumBase

    var power = 0.2
    var motor = 0
    var motorPowers = listOf<Double>(0.0,0.0,0.0,0.0)


    override fun init() {
        base =
            MecanumBase(
                hardwareMap = hardwareMap,
            ).apply { init() }
    }

    override fun loop() {

        if (gamepad1.triangleWasPressed()) {
            motor += 1
            motor = Math.floorMod(motor, 4)
        }

        motorPowers = listOf(
            if (motor == 0) 0.2 else 0.0,
            if (motor == 1) 0.2 else 0.0,
            if (motor == 2) 0.2 else 0.0,
            if (motor == 3) 0.2 else 0.0
        )

        if (gamepad1.x) {
            base.setMotorPowers(motorPowers)
        } else {
            base.setMotorPowers(listOf(0.0,0.0,0.0,0.0))
        }

        telemetry.addData("Motor #", motor)
        telemetry.addData("Motor Powers", motorPowers.toString())
        telemetry.update()
    }
}
