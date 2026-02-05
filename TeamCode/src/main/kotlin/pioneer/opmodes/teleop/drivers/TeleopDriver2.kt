package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.Bot
import pioneer.decode.Artifact
import pioneer.decode.GoalTag
import pioneer.general.AllianceColor
import pioneer.hardware.Turret
import pioneer.hardware.prism.Color
import pioneer.helpers.Chrono
import pioneer.helpers.FileLogger
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import pioneer.vision.AprilTag
import kotlin.math.*

class TeleopDriver2(
    private val gamepad: Gamepad,
    private val bot: Bot,
) {
    private val isAutoTracking = Toggle(false)
    private val isEstimatingSpeed = Toggle(true)
    private val flywheelToggle = Toggle(false)
    private val launchToggle = Toggle(false)
    private val launchPressedTimer = ElapsedTime()
    private var tagShootingTarget = Pose() //Shooting target from goal tag class
    private var offsetShootingTarget = Pose() //Shooting target that has been rotated by manual adjustment
    private var finalShootingTarget = Pose() //Final target that the turret tracks
    private var shootingArtifact = false
    var useAutoTrackOffset = false
    var targetGoal = GoalTag.RED
    var turretAngle = 0.0
    var flywheelSpeed = 0.0
    var manualFlywheelSpeed = 0.0
    var flywheelSpeedOffset = 0.0
    var errorDegrees = 0.0

    fun update() {
        updateFlywheelSpeed()
        handleFlywheel()
        handleTurret()
        handleShootInput()
        processShooting()
        updateIndicatorLED()
    }

    fun onStart() {
        if (bot.allianceColor == AllianceColor.BLUE) {
            targetGoal = GoalTag.BLUE
        }
        tagShootingTarget = targetGoal.shootingPose
        offsetShootingTarget = tagShootingTarget
    }

    fun resetTurretOffsets(){
        flywheelSpeedOffset = 0.0
        useAutoTrackOffset = false
        offsetShootingTarget = tagShootingTarget
        //TODO Sync with driver 1 reset pose
    }

    private fun updateFlywheelSpeed() {
        isEstimatingSpeed.toggle(gamepad.dpad_left)
        if (!isEstimatingSpeed.state){
            if (gamepad.dpad_up){
                manualFlywheelSpeed += 50.0
            }
            if (gamepad.dpad_down){
                manualFlywheelSpeed -= 50.0
            }
            flywheelSpeed = manualFlywheelSpeed
        } else {
            if (gamepad.dpad_up){
                flywheelSpeedOffset += 10.0
            }
            if (gamepad.dpad_down){
                flywheelSpeedOffset -= 10.0
            }
            if (gamepad.left_stick_button) {
                flywheelSpeedOffset = 0.0
            }

            //flywheelSpeed = bot.flywheel!!.estimateVelocity(bot.pinpoint?.pose ?: Pose(), tagShootingTarget, targetGoal.shootingHeight) + flywheelSpeedOffset
            val tagDetections = bot.camera?.getProcessor<AprilTagProcessor>()?.detections
            val ftcPose = tagDetections?.firstOrNull()?.ftcPose
            if (ftcPose != null) {
                flywheelSpeed = bot.flywheel!!.estimateVelocity(
                    hypot(ftcPose.x, ftcPose.y),
                    targetGoal.shootingHeight
                ) + flywheelSpeedOffset
            }
        }
    }

    private fun handleFlywheel() {
        flywheelToggle.toggle(gamepad.dpad_right)
        FileLogger.debug("Teleop Driver 2", flywheelToggle.state.toString())
        FileLogger.debug("Flywheel Speed", flywheelSpeed.toString())
        if (flywheelToggle.state) {
            bot.flywheel?.velocity = flywheelSpeed
        } else {
            bot.flywheel?.velocity = 0.0
        }
    }

    private fun handleTurret() {
        isAutoTracking.toggle(gamepad.cross)
        bot.turret?.mode = if (isAutoTracking.state) Turret.Mode.AUTO_TRACK else Turret.Mode.MANUAL
        if (bot.turret?.mode == Turret.Mode.MANUAL) handleManualTrack() else handleAutoTrack()
    }

    private fun handleShootInput() {
        when {
            gamepad.right_bumper -> shootArtifact(Artifact.PURPLE)
            gamepad.left_bumper -> shootArtifact(Artifact.GREEN)
            gamepad.triangle -> shootArtifact()
        }
    }

    private fun processShooting() {
        if (shootingArtifact && bot.launcher?.isReset == true ) {
            shootingArtifact = false
            bot.spindexer?.popCurrentArtifact()
        }
        if (!flywheelToggle.state) return
        launchToggle.toggle(gamepad.square)
        if (launchToggle.justChanged &&
            bot.spindexer?.withinDetectionTolerance == true &&
            bot.spindexer?.isOuttakePosition == true
        ) {
            bot.launcher?.triggerLaunch()
            shootingArtifact = true
        } else if (launchToggle.justChanged && launchPressedTimer.seconds() < 0.5) {
            bot.launcher?.triggerLaunch()
            shootingArtifact = true
        }
        if (launchToggle.justChanged) launchPressedTimer.reset()
    }

    private fun shootArtifact(artifact: Artifact? = null) {
        // Can't shoot when flywheel isn't moving
        // Start artifact launch sequence
        val moved =
            if (artifact != null) {
                bot.spindexer?.moveToNextOuttake(artifact)
            } else {
                bot.spindexer?.moveToNextOuttake()
            }
    }

    private fun handleManualTrack() {
        if (abs(gamepad.right_stick_x) > 0.02) {
//            turretAngle -= gamepad.right_stick_x.toDouble().pow(3) * chrono.dt/1000.0
            turretAngle -= gamepad.right_stick_x.toDouble().pow(3) / 10.0

        }

        if (gamepad.right_stick_button) {
            turretAngle = 0.0
        }
        bot.turret?.gotoAngle(turretAngle)
    }

    private fun handleAutoTrack() {
        if (bot.turret?.mode == Turret.Mode.AUTO_TRACK) {
            val tagDetections = bot.camera?.getProcessor<AprilTagProcessor>()?.detections

            // FIXME: Might not work
            tagDetections?.filter{
                it.equals(
                    when (bot.allianceColor) {
                        AllianceColor.RED -> GoalTag.RED.id
                        AllianceColor.BLUE -> GoalTag.BLUE.id
                        AllianceColor.NEUTRAL -> GoalTag.RED.id
                    }
                )
            }

            // FIXME: Doesn't work
            val tagErrorDegrees = tagDetections?.firstOrNull()?.ftcPose?.bearing?.times(-1.0)
            if (tagErrorDegrees != null) {
                errorDegrees = tagErrorDegrees
            } else {
                // Update with IMU based on last known error
                val dTheta = (bot.pinpoint?.prevPose?.theta ?: 0.0) - (bot.pinpoint?.pose?.theta ?: 0.0)
                errorDegrees -= dTheta
            }

            bot.turret?.tagTrack(
                errorDegrees,
            )
        }
//        if (abs(gamepad.right_stick_x) > 0.02) {
//            useAutoTrackOffset = true
//            offsetShootingTarget = offsetShootingTarget.rotate(-gamepad.right_stick_x.toDouble().pow(3) / 17.5)
//        }
//        if (gamepad.right_stick_button){
//            useAutoTrackOffset = false
//            offsetShootingTarget = tagShootingTarget
//        }
//        if (useAutoTrackOffset){
//            finalShootingTarget = offsetShootingTarget
//        } else {
//            finalShootingTarget = tagShootingTarget
//        }
    }

    private fun updateIndicatorLED() {
        bot.flywheel?.velocity?.let {
            if (abs(flywheelSpeed - it) < 20.0) {
                bot.led?.setColor(Color.GREEN)
                gamepad.setLedColor(0.0, 1.0, 0.0, -1)
            } else if (it < flywheelSpeed - 20.0){
                bot.led?.setColor(Color.YELLOW)
                gamepad.setLedColor(255.0,165.0,0.0, -1)
            }
            else {
                bot.led?.setColor(Color.RED)
                gamepad.setLedColor(1.0, 0.0, 0.0, -1)
            }
        }
    }
}
