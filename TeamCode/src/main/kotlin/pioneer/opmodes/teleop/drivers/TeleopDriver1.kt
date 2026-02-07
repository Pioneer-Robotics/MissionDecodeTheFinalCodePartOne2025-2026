package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection
import pioneer.Bot
import pioneer.Constants
import pioneer.decode.Points
import pioneer.general.AllianceColor
import pioneer.helpers.Pose
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class TeleopDriver1(
    var gamepad: Gamepad,
    val bot: Bot,
) {
    var drivePower = Constants.Drive.DEFAULT_POWER
    var fieldCentric = false
    private var prevTouchpad = false

    private var prevRightBumper = false
    private var prevLeftBumper = false

    private var intakeEnabled = false
    private var prevCircle = false

    var detection: AprilTagDetection? = null
    var robotPoseTag: Pose? = null
    private lateinit var P: Points

    fun update() {
        drive()
        updateDrivePower()
        updateFieldCentric()
        updateIntake()
        moveSpindexerManual()
        handleSpindexerReset()
        handleResetPose()
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
        val rightBumperPressed = gamepad.right_bumper && !prevRightBumper
        prevRightBumper = gamepad.right_bumper
        val leftBumperPressed = gamepad.left_bumper && !prevLeftBumper
        prevLeftBumper = gamepad.left_bumper
        if (rightBumperPressed) {
            drivePower += 0.1
        }
        if (leftBumperPressed) {
            drivePower -= 0.1
        }
        drivePower = drivePower.coerceIn(0.1, 1.0)
    }

    private fun updateFieldCentric() {
        val touchpadPressed = gamepad.touchpad && !prevTouchpad
        prevTouchpad = gamepad.touchpad
        if (touchpadPressed) {
            fieldCentric = !fieldCentric
        }
    }

    private fun updateIntake() {
        val circlePressed = gamepad.circle && !prevCircle
        prevCircle = gamepad.circle
        if (circlePressed) {
            intakeEnabled = !intakeEnabled
            if (intakeEnabled) {
                bot.spindexer?.moveToNextOpenIntake()
            }
        }
        if (gamepad.dpad_down) {
            bot.intake?.reverse()
        } else {
            if (intakeEnabled) {
                bot.intake?.forward()
            } else {
                bot.intake?.stop()
            }
        }
    }

    private fun moveSpindexerManual() {
//        FileLogger.debug("Teleop Driver 1", "Manual override = ${bot.spindexer?.manualOverride}")
        if (gamepad.right_trigger > 0.1) {
            bot.spindexer?.moveManual(gamepad.right_trigger.toDouble())
        }
        if (gamepad.left_trigger > 0.1) {
            bot.spindexer?.moveManual(-gamepad.left_trigger.toDouble())
        } else if (bot.spindexer?.manualOverride == true) {
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

//        detection = bot.camera?.getProcessor<AprilTagProcessor>()?.detections?.firstOrNull()
//
//        val robotTheta = bot.pinpoint?.pose?.theta ?: return
//        if (detection != null) {
//            val tagDistance = hypot(detection!!.ftcPose.x, detection!!.ftcPose.y)
//            val fieldOffset = Pose(cos(PI/2 + robotTheta), sin(PI/2 + robotTheta)) * tagDistance
//            val tagPosition = when (detection!!.id) {
//                20 -> GoalTag.BLUE.pose
//                24 -> GoalTag.RED.pose
//                else -> return
//            }
//            robotPoseTag = tagPosition - fieldOffset
//        }
//
//        if (gamepad.options && robotPoseTag != null) bot.pinpoint?.reset(robotPoseTag!!.copy(theta=robotTheta))
    }
}