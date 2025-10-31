package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import pioneer.Bot
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import pioneer.Constants.Drive
import pioneer.helpers.Chrono

class TeleopDriver1 (var gamepad: Gamepad, val bot: Bot) {
    private val chrono = Chrono()

    var drivePower = Drive.DEFAULT_POWER
    val fieldCentric: Boolean
        get() = fieldCentricToggle.state

    // Toggles
    private var incDrivePower: Toggle = Toggle(false)
    private var decDrivePower: Toggle = Toggle(false)
    private var fieldCentricToggle: Toggle = Toggle(false)

    // Other variables
    var flywheelSpeed = 0.7

    fun update() {
        chrono.update()
        drive()
        updateDrivePower()
        updateFieldCentric()
        updateFlywheelSpeed()
        flywheel()
        updateLaunchServos()
    }

    private fun drive() {
        val direction = Pose(gamepad.left_stick_x.toDouble(), -gamepad.left_stick_y.toDouble())
        bot.mecanumBase.setDriveVelocity(
            Pose(
                vx = direction.x,
                vy = direction.y,
                omega = gamepad.right_stick_x.toDouble()
            ),
            drivePower,
            Drive.MAX_MOTOR_VELOCITY_TPS
        )
    }

    private fun updateDrivePower() {
        incDrivePower.toggle(gamepad.right_bumper)
        decDrivePower.toggle(gamepad.left_bumper)
        if (incDrivePower.justChanged) {
            drivePower += 0.1
        }
        if (decDrivePower.justChanged) {
            drivePower -= 0.1
        }
        drivePower = drivePower.coerceIn(0.1, 1.0)
    }

    private fun updateFieldCentric() {
        fieldCentricToggle.toggle(gamepad.left_trigger > 0.5 && gamepad.right_trigger > 0.5)
    }

    private fun updateFlywheelSpeed() {
        if (flywheelSpeed < 1.0 && gamepad.dpad_right) {
            flywheelSpeed += 0.25 * chrono.dt / 1000
        }
        if (flywheelSpeed > 0.0 && gamepad.dpad_left) {
            flywheelSpeed -= 0.25 * chrono.dt / 1000
        }
        flywheelSpeed.coerceIn(0.0, 1.0)
    }

    private fun flywheel() {
        if (gamepad.circle) {
            bot.flywheel.setSpeed(-flywheelSpeed)
        } else {
            bot.flywheel.setSpeed(0.0)
        }
    }

    private fun updateLaunchServos() {
        if (gamepad.dpad_up) {
            bot.launchServos.triggerLaunch()
        }
        else if (gamepad.dpad_down) {
            bot.launchServos.triggerRetract()
        }
        bot.launchServos.update()
    }
}
