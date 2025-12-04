package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import pioneer.Bot
import pioneer.Constants
import pioneer.helpers.Pose
import pioneer.helpers.Toggle

class TeleopDriver1(
    var gamepad: Gamepad,
    val bot: Bot,
) {
    var drivePower = Constants.Drive.DEFAULT_POWER
    val fieldCentric: Boolean
        get() = fieldCentricToggle.state

    // Toggles
    private var incDrivePower: Toggle = Toggle(false)
    private var decDrivePower: Toggle = Toggle(false)
    private var fieldCentricToggle: Toggle = Toggle(false)
    private var intakeToggle: Toggle = Toggle(false)

    fun update() {
        drive()
        updateDrivePower()
        updateFieldCentric()
        updateIntake()
        handleSpindexerReset()
        bot.spindexer?.update()
    }

    private fun drive() {
        val direction = Pose(gamepad.left_stick_x.toDouble(), -gamepad.left_stick_y.toDouble())
        bot.mecanumBase?.setDrivePower(
            Pose(
                vx = direction.x,
                vy = direction.y,
                omega = gamepad.right_stick_x.toDouble(),
            ),
            drivePower,
            Constants.Drive.MAX_MOTOR_VELOCITY_TPS,
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

    private fun updateIntake() {
        intakeToggle.toggle(gamepad.circle)
        if (gamepad.dpad_down) {
            bot.intake?.reverse()
        } else {
            if (intakeToggle.state) {
                bot.intake?.forward()
            } else {
                bot.intake?.stop()
            }
        }
        if (intakeToggle.justChanged && intakeToggle.state) {
            bot.spindexer?.moveToNextOpenIntake()
        }
    }

    private fun handleSpindexerReset() {
        if (gamepad.share) {
            bot.spindexer?.reset()
        }
    }
}
