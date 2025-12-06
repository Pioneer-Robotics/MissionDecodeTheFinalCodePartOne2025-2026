package pioneer.opmodes.teleop

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Constants
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import pioneer.opmodes.BaseOpMode

@TeleOp(name = "Basic Teleop")
class BasicTeleop : BaseOpMode() {
    private var drivePower = Constants.Drive.DEFAULT_POWER
    private var incDrivePower: Toggle = Toggle(false)
    private var decDrivePower: Toggle = Toggle(false)

    override fun onLoop() {
        drive()
        updateDrivePower()
    }

    private fun drive() {
        val direction = Pose(gamepad1.left_stick_x.toDouble(), -gamepad1.left_stick_y.toDouble())
        bot.mecanumBase?.setDrivePower(
            Pose(
                vx = direction.x,
                vy = direction.y,
                omega = gamepad1.right_stick_x.toDouble(),
            ),
            drivePower,
            Constants.Drive.MAX_MOTOR_VELOCITY_TPS,
        )
    }

    private fun updateDrivePower() {
        incDrivePower.toggle(gamepad1.right_bumper)
        decDrivePower.toggle(gamepad1.left_bumper)
        if (incDrivePower.justChanged) {
            drivePower += 0.1
        }
        if (decDrivePower.justChanged) {
            drivePower -= 0.1
        }
        drivePower = drivePower.coerceIn(0.1, 1.0)
    }
}
