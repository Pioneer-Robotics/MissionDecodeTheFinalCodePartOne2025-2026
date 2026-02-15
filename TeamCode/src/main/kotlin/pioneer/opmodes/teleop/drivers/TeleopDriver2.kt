package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.Bot
import pioneer.Constants
import pioneer.decode.Artifact
import pioneer.decode.GoalTag
import pioneer.general.AllianceColor
import pioneer.hardware.Turret
import org.firstinspires.ftc.teamcode.prism.Color
import pioneer.helpers.FileLogger
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import pioneer.helpers.next
import kotlin.math.*

class TeleopDriver2(
    private val gamepad: Gamepad,
    private val bot: Bot,
) {
    private val isAutoTracking = Toggle(false)
    private val isEstimatingSpeed = Toggle(true)
    private val flywheelToggle = Toggle(false)
    private val launchToggle = Toggle(false)
    private val multiShotToggle = Toggle(false)
    private val switchOperatingModeToggle = Toggle(false)

    private val launchPressedTimer = ElapsedTime()
    private var tagShootingTarget = Pose()
    private var offsetShootingTarget = Pose()
    private var finalShootingTarget = Pose()
    private var turretPose = Pose()

    private var singleShotActive = false
    private var waitingForLauncherReset = false

    var multishotState = MultishotState.IDLE

    enum class MultishotState {
        IDLE,
        RAPID_FIRING
    }

    var useAutoTrackOffset = false
    var targetGoal = GoalTag.RED
    var turretAngle = 0.0
    var estimatedFlywheelSpeed = 0.0
    var finalFlywheelSpeed = 0.0
    var manualFlywheelSpeed = 0.0
    var flywheelSpeedOffset = 0.0
    var errorDegrees: Double? = 0.0
    var flywheelShouldFloat = true

    fun update() {
        updateFlywheelOperatingMode()
        updateFlywheelSpeed()
        updateTurretPose()
        handleFlywheel()
        handleTurret()
        handleShootInput()
        handleMultiShot()
        processSingleShot()
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
    }

    private fun updateFlywheelOperatingMode(){
        switchOperatingModeToggle.toggle(gamepad.triangle)
        if (switchOperatingModeToggle.justChanged){
            bot.flywheel?.operatingMode = bot.flywheel?.operatingMode?.next()!!
        }
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
            estimatedFlywheelSpeed = manualFlywheelSpeed
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

            val tagDetections = bot.camera?.getProcessor<AprilTagProcessor>()?.detections
            val ftcPose = tagDetections?.firstOrNull()?.ftcPose
            if (ftcPose != null) {
                estimatedFlywheelSpeed = bot.flywheel!!.estimateVelocity(
                    hypot(ftcPose.x, ftcPose.y),
                    targetGoal.shootingHeight
                ) + flywheelSpeedOffset
            }
        }
    }

    private fun updateTurretPose(){
        turretPose = bot.pinpoint?.pose !!+ (Pose(Constants.Turret.OFFSET * sin(bot.pinpoint?.pose!!.theta), Constants.Turret.OFFSET * cos(bot.pinpoint?.pose!!.theta)))
    }

    private fun handleFlywheel() {
        flywheelToggle.toggle(gamepad.dpad_right)
        FileLogger.debug("Teleop Driver 2", flywheelToggle.state.toString())
        FileLogger.debug("Flywheel Speed", finalFlywheelSpeed.toString())

        if (flywheelToggle.state) {
            finalFlywheelSpeed = estimatedFlywheelSpeed
        } else {
            if (flywheelToggle.justChanged){
                flywheelShouldFloat = true
            }

            if (flywheelShouldFloat && bot.flywheel?.velocity!! < bot.flywheel!!.idleVelocity){
                flywheelShouldFloat = false
            }

            if (flywheelShouldFloat){
                finalFlywheelSpeed = 0.0
            } else {
                finalFlywheelSpeed = bot.flywheel!!.idleVelocity
            }
        }

        bot.flywheel?.velocity = finalFlywheelSpeed
    }

    private fun handleTurret() {
        isAutoTracking.toggle(gamepad.cross)
        bot.turret?.mode = if (isAutoTracking.state) Turret.Mode.AUTO_TRACK else Turret.Mode.MANUAL
        if (bot.turret?.mode == Turret.Mode.MANUAL) handleManualTrack() else handleAutoTrack()
    }

    private fun handleShootInput() {
        if (singleShotActive || waitingForLauncherReset) return

        when {
            gamepad.right_bumper -> requestColorShot(Artifact.PURPLE)
            gamepad.left_bumper -> requestColorShot(Artifact.GREEN)
        }
    }

    private fun requestColorShot(artifact: Artifact) {
        val moved = bot.spindexer?.moveToNextOuttake(artifact)
        if (moved == true) {
            singleShotActive = true
            FileLogger.info("TeleopDriver2", "Requested $artifact shot - positioning...")
        } else {
            FileLogger.warn("TeleopDriver2", "No $artifact ball available!")
            gamepad.rumble(100)
        }
    }

    private fun handleMultiShot() {
        multiShotToggle.toggle(gamepad.touchpad)

        when(multishotState) {
            MultishotState.IDLE -> {
                if (multiShotToggle.justChanged && gamepad.touchpad) {
                    val atSpeed = bot.flywheel?.velocity?.let {
                        abs(finalFlywheelSpeed - it) < 20.0
                    } ?: false

                    if (atSpeed && !bot.spindexer?.isEmpty!!) {
                        FileLogger.debug("TeleopDriver2", "Starting RAPID FIRE")
                        bot.spindexer?.startRapidFire()
                        multishotState = MultishotState.RAPID_FIRING
                    } else if (!atSpeed) {
                        FileLogger.warn("TeleopDriver2", "Flywheel not at speed!")
                        gamepad.rumble(200)
                    } else {
                        FileLogger.warn("TeleopDriver2", "No balls to shoot!")
                        gamepad.rumble(200)
                    }
                }
            }

            MultishotState.RAPID_FIRING -> {
                if (multiShotToggle.justChanged && gamepad.touchpad) {
                    FileLogger.debug("TeleopDriver2", "Canceling rapid fire")
                    bot.spindexer?.stopRapidFire()
                    multishotState = MultishotState.IDLE
                }

                if (!bot.spindexer?.isRapidFiring!!) {
                    FileLogger.debug("TeleopDriver2", "Rapid fire complete")
                    multishotState = MultishotState.IDLE
                }
            }
        }

        if (bot.spindexer?.isEmpty == true) {
            multishotState = MultishotState.IDLE
        }
    }

    private fun processSingleShot() {
        if (!flywheelToggle.state) return

        if (waitingForLauncherReset && bot.launcher?.isReset == true) {
            waitingForLauncherReset = false
            singleShotActive = false
            bot.spindexer?.popCurrentArtifact(false)
            FileLogger.info("TeleopDriver2", "Single shot complete")
        }

        if (waitingForLauncherReset) return

        if (singleShotActive && bot.spindexer?.isReadyToShoot == true) {
            launchToggle.toggle(gamepad.square)

            if (launchToggle.justChanged && gamepad.square) {
                val atSpeed = bot.flywheel?.velocity?.let {
                    abs(finalFlywheelSpeed - it) < 20.0
                } ?: false

                if (atSpeed) {
                    bot.spindexer?.triggerSingleShot()
                    bot.launcher?.triggerLaunch()
                    waitingForLauncherReset = true
                    FileLogger.info("TeleopDriver2", "Single shot triggered!")
                } else {
                    FileLogger.warn("TeleopDriver2", "Flywheel not at speed!")
                    gamepad.rumble(100)
                }
            }
        }
    }

    private fun handleManualTrack() {
        if (abs(gamepad.right_stick_x) > 0.02) {
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

            FileLogger.debug("TeleopDriver2", "Tag Detections: ${tagDetections?.map { it.id }?.joinToString { ", " }}")

            val filteredDetections = tagDetections?.filter{
                it.id == when (bot.allianceColor) {
                    AllianceColor.RED -> GoalTag.RED.id
                    AllianceColor.BLUE -> GoalTag.BLUE.id
                    AllianceColor.NEUTRAL -> GoalTag.RED.id
                }
            }

            FileLogger.debug("TeleopDriver2", "Tag Detections: ${filteredDetections?.map { it.id }?.joinToString { ", " }}")

            errorDegrees = filteredDetections?.firstOrNull()?.ftcPose?.bearing
            if (errorDegrees != null) {
                bot.turret?.tagTrack(
                    errorDegrees!!.times(-1.0),
                )
            } else {
                bot.turret?.autoTrack(
                    bot.pinpoint?.pose ?: Pose(),
                    targetGoal.shootingPose
                )
            }
        }
    }

    private fun updateIndicatorLED() {
        if (flywheelToggle.state){
            bot.flywheel?.velocity?.let {
                if (abs(estimatedFlywheelSpeed - it) < 20.0) {
                    bot.led?.setColor(Color.GREEN)
                    gamepad.setLedColor(0.0, 1.0, 0.0, -1)
                } else if (it < estimatedFlywheelSpeed - 20.0){
                    bot.led?.setColor(Color.YELLOW)
                    gamepad.setLedColor(255.0,165.0,0.0, -1)
                }
                else {
                    bot.led?.setColor(Color.RED)
                    gamepad.setLedColor(1.0, 0.0, 0.0, -1)
                }
            }
        } else {
            gamepad.setLedColor(255.0,255.0,255.0, -1)
            bot.led?.setColor(Color.WHITE)
        }
    }
}