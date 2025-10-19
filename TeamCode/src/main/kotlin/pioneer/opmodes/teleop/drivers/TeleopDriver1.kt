package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import pioneer.Bot
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import pioneer.Constants.Drive.DEFAULT_POWER

class TeleopDriver1 (var gamepad: Gamepad, val bot: Bot) {
    var drivePower = DEFAULT_POWER
    val fieldCentric: Boolean
        get() = fieldCentricToggle.state

    // Toggles
    private var incDrivePower: Toggle = Toggle(false)
    private var decDrivePower: Toggle = Toggle(false)
    private var fieldCentricToggle: Toggle = Toggle(false)

    fun update() {
        drive()
        updateDrivePower()
        updateFieldCentric()
        flywheelSpeed()
        updateLaunchServos()
    }

    fun drive() {
        val direction = Pose(gamepad.left_stick_x.toDouble(), -gamepad.left_stick_y.toDouble())
        bot.mecanumBase.setDrivePower(
            Pose(
                vx = direction.x,
                vy = direction.y,
                omega = gamepad.right_stick_x.toDouble()
            ),
            drivePower
        )
    }

    fun updateDrivePower() {
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

    fun updateFieldCentric() {
        fieldCentricToggle.toggle(gamepad.left_trigger > 0.5 && gamepad.right_trigger > 0.5)
    }

    fun flywheelSpeed() {
        if (gamepad.circle) {
            bot.flywheel.setSpeed(0.67)
        } else {
            bot.flywheel.setSpeed(0.0)
        }
    }

    fun updateLaunchServos() {
        if (gamepad.dpad_up) {
            bot.launchServos.triggerLaunch()
        }
        else if (gamepad.dpad_down) {
            bot.launchServos.triggerRetract()
        }
        bot.launchServos.update()
    }
}
