package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import com.qualcomm.robotcore.util.ElapsedTime
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

    var tiltServoToggle = Toggle(false)
    var driveDisabled = false
    var ptoTimer = ElapsedTime()

    var tiltDrive = Toggle(startState = false)
    var tiltTargetDistance = 0

    fun update() {
        drive()
        updateDrivePower()
        updateFieldCentric()
        updateIntake()
        moveSpindexerManual()
        handleSpindexerReset()
        handleResetPose()
//        handleTilt()
    }

    private fun drive() {
        var direction = Pose(gamepad.left_stick_x.toDouble(), -gamepad.left_stick_y.toDouble())
        if (fieldCentric) {
            var angle = atan2(direction.y, direction.x) - bot.pinpoint?.pose!!.theta
            angle += when (bot.allianceColor) {
                AllianceColor.BLUE -> PI/2
                AllianceColor.RED -> -PI/2
                AllianceColor.NEUTRAL -> 0.0
            }
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
            Constants.Drive.MAX_MOTOR_VELOCITY_TPS
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
        if (gamepad.right_trigger > 0.1) {
            bot.spindexer?.moveManual(gamepad.right_trigger.toDouble())
        }
        if (gamepad.left_trigger > 0.1) {
            bot.spindexer?.moveManual(-gamepad.left_trigger.toDouble())
        } else if (bot.spindexer?.isManualOverride == true) {
            bot.spindexer?.moveManual(0.0)
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
                bot.pinpoint?.reset(Pose(-86.7, -99.0, theta = 0.0))
            } else {
                bot.pinpoint?.reset(Pose(86.7, -99.0, theta = 0.0))
            }
        }
    }

    private fun handleTilt(){
        tiltServoToggle.toggle(gamepad.dpad_up && gamepad.cross)
        tiltDrive.toggle(button = gamepad.dpad_down && gamepad.cross)

        if (tiltServoToggle.justChanged) {
            if (tiltServoToggle.state) {
                bot.servosPTO?.dropServos()

            } else {
                bot.servosPTO?.raiseServos()
            }
        }

        if (tiltDrive.justChanged) {
            ptoTimer.reset()
            if (tiltDrive.state) {
                driveDisabled = true
                bot.mecanumBase?.stop()
                tiltTargetDistance = (bot.mecanumBase?.getMotorPositions()[0] ?: 0) + 500
                bot.mecanumBase?.setMotorPowers(listOf(0.0, 0.0, 0.0, 0.2))

            }
            else  {
                driveDisabled = false
            }
        }

//        if (tiltDrive.state && ptoTimer.milliseconds() > 250) {
//            bot.mecanumBase?.setMotorPowers(listOf(0.0, 0.2, 0.0, -0.2))
//        }

        if (!tiltDrive.state){
            bot.mecanumBase?.setMotorPowers(listOf(0.0,0.0,0.0,0.0))
        }

    }
}
