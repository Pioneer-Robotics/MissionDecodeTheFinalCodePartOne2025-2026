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
    private val useVelocityAdj = Toggle(false)
    private val launchPressedTimer = ElapsedTime()
    private var tagShootingTarget = Pose() //Shooting target from goal tag class
    private var offsetShootingTarget = Pose() //Shooting target that has been rotated by manual adjustment
    private var finalShootingTarget = Pose() //Final target that the turret tracks
    private var turretPose = Pose()
    private var shootingArtifact = false
    var useAutoTrackOffset = false
    var targetGoal = GoalTag.RED
    var turretAngle = 0.0
    var estimatedFlywheelSpeed = 0.0
    var finalFlywheelSpeed = 0.0
    var manualFlywheelSpeed = 0.0
    var flywheelSpeedOffset = 0.0
    var errorDegrees: Double? = 0.0
    var shootCommanded = false
    var triggerMultishot = false

    var multishotState = MultishotState.IDLE

    enum class MultishotState {
        IDLE,
        MOVING,
        SHOOTING
    }
    var flywheelShouldFloat = true

    fun update() {
        updateFlywheelOperatingMode()
        updateFlywheelSpeed()
        updateTurretPose()
        handleFlywheel()
        handleTurret()
        handleShootInput()
        handleMultiShot()
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

            //flywheelSpeed = bot.flywheel!!.estimateVelocity(bot.pinpoint?.pose ?: Pose(), tagShootingTarget, targetGoal.shootingHeight) + flywheelSpeedOffset
            val tagDetections = bot.camera?.getProcessor<AprilTagProcessor>()?.detections
            val ftcPose = tagDetections?.firstOrNull()?.ftcPose
            if (ftcPose != null) {
                estimatedFlywheelSpeed = bot.flywheel!!.estimateVelocity(
                    hypot(ftcPose.x, ftcPose.y),
                    targetGoal.shootingHeight
                ) + flywheelSpeedOffset
            } else {
                bot.flywheel!!.estimateVelocity(turretPose, tagShootingTarget, targetGoal.shootingHeight)
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
//                    finalFlywheelSpeed = 0.0
                }

                if (flywheelShouldFloat && bot.flywheel?.velocity!! < bot.flywheel!!.idleVelocity){
                    flywheelShouldFloat = false
//                    finalFlywheelSpeed = bot.flywheel!!.idleVelocity
                }

//                if (!flywheelShouldFloat){
//                    finalFlywheelSpeed = bot.flywheel!!.idleVelocity
//                } else {
//                    finalFlywheelSpeed = 0.0
//                }

                if (flywheelShouldFloat){
                    finalFlywheelSpeed = 0.0
                } else {
                    finalFlywheelSpeed = bot.flywheel!!.idleVelocity
                }

//                if (it < bot.flywheel!!.idleVelocity)
//                    finalFlywheelSpeed = bot.flywheel!!.idleVelocity
//                else {
//                    finalFlywheelSpeed = 0.0
//                }
            }

            bot.flywheel?.velocity = finalFlywheelSpeed
    }


    private fun handleTurret() {
        isAutoTracking.toggle(gamepad.cross)
        bot.turret?.mode = if (isAutoTracking.state) Turret.Mode.AUTO_TRACK else Turret.Mode.MANUAL
        if (bot.turret?.mode == Turret.Mode.MANUAL) handleManualTrack() else handleAutoTrack()
    }

    private fun handleShootInput() {
        if (shootingArtifact) return
        when {
            gamepad.right_bumper -> shootArtifact(Artifact.PURPLE)
            gamepad.left_bumper -> shootArtifact(Artifact.GREEN)
//            gamepad.triangle -> shootArtifact()
        }
    }

    private fun handleMultiShot() {
        multiShotToggle.toggle(gamepad.touchpad)

        when(multishotState) {
            MultishotState.IDLE -> {
                if (multiShotToggle.justChanged && gamepad.touchpad) {
                    multishotState = MultishotState.MOVING
                    FileLogger.debug("Teleop Driver 2", "Should have changed to MOVING")
                }
            }
            MultishotState.MOVING -> {
                shootArtifact()
                if (multiShotToggle.justChanged && gamepad.touchpad) {
                    FileLogger.debug("Teleop Driver 2", "Changed back to IDLE")
                    multishotState = MultishotState.IDLE
                }

                val atSpeed = bot.flywheel?.velocity?.let {
                    if (abs(finalFlywheelSpeed - it) < 20.0) { true }
                    else { false }
                }

                if (bot.spindexer?.reachedTarget == true && atSpeed == true) {
                    multishotState = MultishotState.SHOOTING
                    triggerMultishot = true
                }
            }
            MultishotState.SHOOTING -> {
                if (multiShotToggle.justChanged && gamepad.touchpad) {
                    multishotState = MultishotState.IDLE
                }
                if (shootingArtifact) {
                    triggerMultishot = false
                }
                if (!shootingArtifact && !triggerMultishot) {
                    multishotState = MultishotState.MOVING
                }
            }
        }

        if (bot.spindexer?.isEmpty == true) {
            multishotState = MultishotState.IDLE
        }
    }

    private fun processShooting() {
        if (shootingArtifact && bot.launcher?.isReset == true ) {
            shootingArtifact = false
            bot.spindexer?.popCurrentArtifact(false)
        }
        if (!flywheelToggle.state) return

        launchToggle.toggle(gamepad.square)
        shootCommanded = launchToggle.justChanged || triggerMultishot

        if (shootCommanded &&
            bot.spindexer?.withinDetectionTolerance == true &&
            bot.spindexer?.isOuttakePosition == true
        ) {
            bot.launcher?.triggerLaunch()
            shootingArtifact = true
        } else if (shootCommanded && launchPressedTimer.seconds() < 0.5) {
            bot.launcher?.triggerLaunch()
            shootingArtifact = true
        }
        if (shootCommanded) launchPressedTimer.reset()
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

            FileLogger.debug("TeleopDriver2", "Tag Detections: ${tagDetections?.map { it.id }?.joinToString { ", " }}")

            // Only look at goal tag for the current alliance
            val filteredDetections = tagDetections?.filter{
                it.id == when (bot.allianceColor) {
                    AllianceColor.RED -> GoalTag.RED.id
                    AllianceColor.BLUE -> GoalTag.BLUE.id
                    AllianceColor.NEUTRAL -> GoalTag.RED.id
                }
            }

            FileLogger.debug("TeleopDriver2", "Tag Detections: ${filteredDetections?.map { it.id }?.joinToString { ", " }}")

            val ftcPose = filteredDetections?.firstOrNull()?.ftcPose
            errorDegrees = ftcPose?.bearing
//            if (errorDegrees != null) {
//                bot.turret?.tagTrack(
//                    errorDegrees!!.times(-1.0),
//                )
//            } else {
//                // No tag detected, use last known target
//                // TODO: fix tag loss logicx
//                bot.turret?.autoTrack(
//                    bot.pinpoint?.pose ?: Pose(),
//                    targetGoal.shootingPose
//                )
//            }
            if (errorDegrees != null) {
                bot.turret?.velocityTagTrack(
                    errorDegrees!!.times(1.0),
                    bot.pinpoint!!.pose,
                    bot.flywheel!!.getFlywheelV0(
                        hypot(ftcPose!!.x, ftcPose!!.y),
                        targetGoal.shootingHeight
                    )
                )
            } else {
                // No tag detected, use last known target
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
