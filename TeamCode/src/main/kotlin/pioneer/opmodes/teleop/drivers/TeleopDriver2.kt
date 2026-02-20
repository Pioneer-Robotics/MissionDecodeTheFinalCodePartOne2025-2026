package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.Bot
import pioneer.Constants
import pioneer.decode.GoalTag
import pioneer.general.AllianceColor
import pioneer.hardware.Turret
import org.firstinspires.ftc.teamcode.prism.Color
import pioneer.decode.Motif
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
    val isEstimatingSpeed = Toggle(true)
    private val flywheelToggle = Toggle(false)
    private val launchToggle = Toggle(false)
    private val multiShotToggle = Toggle(false)
    private val switchOperatingModeToggle = Toggle(false)
    private val launchPressedTimer = ElapsedTime()
    private var tagShootingTarget = Pose() //Shooting target from goal tag class
    private var offsetShootingTarget = Pose() //Shooting target that has been rotated by manual adjustment
//    private var finalShootingTarget = Pose() //Final target that the turret tracks
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
    var shotCounter = 0

    var motif: Motif = Motif(21)
    var shootAll = false

    var flywheelShouldFloat = true
    val shootTimer = ElapsedTime()
    val minShotTime = 0.75

    fun update() {
        updateFlywheelOperatingMode()
        updateFlywheelSpeed()
        updateTurretPose()
        handleFlywheel()
        handleTurret()
        updateMotif()
        readyOuttake()
        handleShootInput()
        handleShootAll()
        updateIndicatorLED()
    }

    fun onStart() {
        if (bot.allianceColor == AllianceColor.BLUE) {
            targetGoal = GoalTag.BLUE
        }
        tagShootingTarget = targetGoal.shootingPose
        offsetShootingTarget = tagShootingTarget
        shootTimer.reset()
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
                estimatedFlywheelSpeed = bot.flywheel!!.estimateVelocity(turretPose, tagShootingTarget, targetGoal.shootingHeight) + flywheelSpeedOffset
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

    private fun updateMotif(){
        if (gamepad.rightBumperWasPressed()){
            motif = motif.nextMotif()!!
        }

        if (gamepad.leftBumperWasPressed()){
            motif = motif.prevMotif()!!
        }
    }

    private fun readyOuttake(){
        if (gamepad.circleWasPressed()){
            bot.spindexer?.readyOuttake(motif)
        }
    }

    private fun handleShootInput() {
        if (shootingArtifact) return
        when {
            gamepad.square -> {
                if (shootTimer.seconds() > minShotTime) {
                    shootTimer.reset()
                    bot.spindexer?.shootNext()
                }
            }
            gamepad.touchpadWasPressed() -> shootAll = true
        }
    }

    private fun handleShootAll() {
        if (!shootAll) return
        // Add a minimum delay and check that flywheel is at speed
        if (shootTimer.seconds() > minShotTime && flywheelAtSpeed()) {
            bot.spindexer?.shootNext()
            shootTimer.reset()
            shotCounter += 1

        }
        // Check if the spindexer is empty and the last shot has cleared
        if (bot.spindexer?.isEmpty == true && bot.spindexer?.reachedTarget == true) {
            shootAll = false
            shotCounter = 0
        }
    }

    fun flywheelAtSpeed(): Boolean {
        val fw = bot.flywheel ?: return false
        return (bot.flywheel?.velocity ?: 0.0) > (fw.targetVelocity - 20) &&
                (bot.flywheel?.velocity ?: 0.0) < (fw.targetVelocity + 20)
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

            errorDegrees = filteredDetections?.firstOrNull()?.ftcPose?.bearing
            if (errorDegrees != null) {
                bot.turret?.tagTrack(
                    errorDegrees!!.times(-1.0),
                )
            } else {
                // No tag detected, use last known target
                // TODO: fix tag loss logic
                bot.turret?.autoTrack(
                    bot.pinpoint?.pose ?: Pose(),
                    targetGoal.shootingPose
                )
            }
        }
    }

    private fun updateIndicatorLED() {
        // Display current target motif on bot LED:
        bot.led?.displayArtifacts(motif.artifacts)

        // Display flywheel status on LED:
        // GREEN --> at speed
        // YELLOW --> under speed
        // RED --> over speed
        if (flywheelToggle.state){
            bot.flywheel?.velocity?.let {
                if (abs(estimatedFlywheelSpeed - it) < 40.0) {
                    bot.led?.setColor(Color.GREEN)
                    gamepad.setLedColor(0.0, 1.0, 0.0, -1)
                } else if (it < estimatedFlywheelSpeed - 40.0){
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
