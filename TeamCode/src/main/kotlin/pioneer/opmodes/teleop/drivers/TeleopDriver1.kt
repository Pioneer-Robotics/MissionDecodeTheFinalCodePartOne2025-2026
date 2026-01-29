package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection
import pioneer.Bot
import pioneer.Constants
import pioneer.decode.Points
import pioneer.general.AllianceColor
import pioneer.helpers.FileLogger
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class TeleopDriver1(
    var gamepad: Gamepad,
    val bot: Bot,
    private val driver2: pioneer.opmodes.teleop.drivers.TeleopDriver2? = null,  // NEW: Reference to driver2 for sync
) {
    var drivePower = Constants.Drive.DEFAULT_POWER
    val fieldCentric: Boolean
        get() = fieldCentricToggle.state

    // Toggles
    private var incDrivePower: Toggle = Toggle(false)
    private var decDrivePower: Toggle = Toggle(false)
    private var fieldCentricToggle: Toggle = Toggle(false)
    private var intakeToggle: Toggle = Toggle(false)

    var detection: AprilTagDetection? = null
    var robotPoseTag: Pose? = null

    // NEW: Field points for drift correction reset
    private lateinit var P: Points

    // NEW: Drift correction state
    private var lastResetTime = 0L
    private val RESET_COOLDOWN_MS = 2000L

    fun update() {
        drive()
        updateDrivePower()
        updateFieldCentric()
        updateIntake()
        moveSpindexerManual()
        handleSpindexerReset()
        handleResetPose()
        handleDriftCorrectionReset()  // NEW: Add drift correction reset
    }

    // NEW: Initialize field points when alliance changes
    fun updateFieldPoints() {
        P = Points(bot.allianceColor)
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
            // NEW: Sync with driver2 when resetting
            driver2?.resetTurretOffsets()
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

    // NEW: Drift correction reset with BACK+START
    private fun handleDriftCorrectionReset() {
        // Check if BACK + START are both pressed
        if (gamepad.back && gamepad.start) {
            val currentTime = System.currentTimeMillis()

            // Cooldown to prevent accidental double-resets
            if (currentTime - lastResetTime < RESET_COOLDOWN_MS) {
                return
            }

            // Select position based on D-pad
            val position = when {
                gamepad.dpad_up -> P.SHOOT_GOAL_CLOSE      // Shooting position
                gamepad.dpad_down -> P.START_GOAL           // Starting position
                gamepad.dpad_left -> P.COLLECT_AUDIENCE     // Audience collection
                gamepad.dpad_right -> P.COLLECT_GOAL        // Goal collection
                else -> P.SHOOT_GOAL_CLOSE                  // Default: shooting
            }

            // Reset odometry
            bot.pinpoint?.reset(position)

            // Sync with driver2
            driver2?.resetTurretOffsets()

            // Haptic feedback
            gamepad.rumble(500)

            // Update last reset time
            lastResetTime = currentTime

            // Log for debugging
            FileLogger.debug("TeleopDriver1", "DRIFT CORRECTION RESET to: $position")
        }
    }
}