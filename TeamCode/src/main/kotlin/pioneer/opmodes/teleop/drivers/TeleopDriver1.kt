package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import pioneer.Bot
import pioneer.Constants
import pioneer.general.AllianceColor
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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
        moveSpindexerManual()
        handleCancelLastIntake()
        handleSpindexerReset()
        handleResetPose()
        bot.spindexer?.update()

        if (gamepad.right_stick_button) {
            bot.spindexer?.resetMotorPosition()
        }
    }

    private fun drive() {
        var direction = Pose(gamepad.left_stick_x.toDouble(), -gamepad.left_stick_y.toDouble())
        if (fieldCentric) {
            var angle = atan2(direction.y, direction.x) - bot.pinpoint?.pose!!.theta
            angle += if (bot.allianceColor == AllianceColor.BLUE) PI / 2 else -PI / 2
            val mag = direction.getLength()
            direction = Pose(mag * cos(angle), mag * sin(angle))
        }
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
        fieldCentricToggle.toggle(gamepad.touchpad)
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

    private fun moveSpindexerManual() {
        // FIXME: Manual moving won't reset power back to 0
        if (gamepad.right_trigger > 0.1) {
            bot.spindexer?.moveManual(gamepad.right_trigger.toDouble())
        }
        if (gamepad.left_trigger > 0.1) {
            bot.spindexer?.moveManual(-gamepad.left_trigger.toDouble())
        }
    }

    private fun handleCancelLastIntake() {
        if (gamepad.cross) {
            bot.spindexer?.cancelLastIntake()
        }
    }

    private fun handleSpindexerReset() {
        if (gamepad.share) {
            bot.spindexer?.reset()
        }
    }

    private fun handleResetPose() {
        if (gamepad.options) {
            if (bot.allianceColor == AllianceColor.RED) {
                bot.pinpoint?.reset(Pose(-80.0, -95.0, theta = 0.1))
            } else {
                bot.pinpoint?.reset(Pose(80.0, -95.0, theta = 0.1))
            }
        }
    }
}
