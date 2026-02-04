package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Bot
import pioneer.Constants
import pioneer.FlywheelOperatingMode
import pioneer.decode.Artifact
import pioneer.decode.GoalTag
import pioneer.general.AllianceColor
import pioneer.hardware.Flywheel
import pioneer.hardware.Turret
import pioneer.hardware.prism.Color
import pioneer.helpers.Chrono
import pioneer.helpers.MathUtils
import pioneer.helpers.Pose
import pioneer.helpers.TestMatrixLogger
import pioneer.helpers.Toggle
import pioneer.helpers.next
import kotlin.math.*

class TeleopDriver2(
    private val gamepad: Gamepad,
    private val bot: Bot,
    private var testLogger: TestMatrixLogger? = null  // ✅ Optional test logger
) {
    private val chrono = Chrono(autoUpdate = false)
    private val isAutoTracking = Toggle(false)
    private val isEstimatingSpeed = Toggle(true)
    private val flywheelToggle = Toggle(false)
    private val launchToggle = Toggle(false)
    private val flywheelOperateModeToggle = Toggle(false)
    private val launchPressedTimer = ElapsedTime()
    private var tagShootingTarget = Pose()
    private var offsetShootingTarget = Pose()
    private var finalShootingTarget = Pose()
    private var shootingArtifact = false
    var useAutoTrackOffset = false
    var targetGoal = GoalTag.RED
    var turretAngle = 0.0
    var flywheelSpeed = 0.0
    var manualFlywheelSpeed = 0.0
    var flywheelSpeedOffset = 0.0

    // Multi-shot state machine
    private enum class MultiShotState {
        IDLE,
        WAITING_FOR_LAUNCHER,
        WAITING_FOR_SPINDEXER,
        COMPLETE
    }

    private var multiShotState = MultiShotState.IDLE
    private var multiShotCount = 0
    private var multiShotTarget = 0
    private var multiShotUseFastMode = false
    private val multiShotTimer = ElapsedTime()

    // ✅ NEW: Shot tracking for statistics
    private var currentShotNumber = 0
    private var totalShotsFired = 0
    private var totalShotsHit = 0  // Would need vision/sensor feedback to track

    // Telemetry info
    var multiShotStatus = ""
        private set

    fun update() {
        handleFlywheelOperateMode()
        updateFlywheelSpeed()
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

        // Set flywheel to idle state at start
        bot.flywheel?.state = when (Constants.Flywheel.OPERATING_MODE) {
            FlywheelOperatingMode.ALWAYS_IDLE -> Flywheel.FlywheelState.IDLE
            FlywheelOperatingMode.SMART_IDLE -> Flywheel.FlywheelState.IDLE
            FlywheelOperatingMode.CONSERVATIVE_IDLE -> Flywheel.FlywheelState.IDLE
            FlywheelOperatingMode.FULL_OFF -> Flywheel.FlywheelState.OFF
        }
    }

    fun resetTurretOffsets(){
        flywheelSpeedOffset = 0.0
        useAutoTrackOffset = false
        offsetShootingTarget = tagShootingTarget
    }

    // ✅ NEW: Get shot accuracy for test reporting
    fun getShotAccuracy(): Double {
        return if (totalShotsFired > 0) {
            (totalShotsHit.toDouble() / totalShotsFired * 100.0)
        } else {
            0.0
        }
    }

    //Handle flywheel operating mode
    private fun handleFlywheelOperateMode(){
        flywheelOperateModeToggle.toggle(gamepad.triangle)
        if (flywheelOperateModeToggle.justChanged){
            Constants.Flywheel.OPERATING_MODE.next()
            //FIXME KInd of sketchy since changing constants
            //FIXME Doesnt even work
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

            // ✅ UPDATED: estimateVelocity now includes battery compensation
            flywheelSpeed = bot.flywheel!!.estimateVelocity(
                bot.pinpoint?.pose ?: Pose(),
                tagShootingTarget,
                targetGoal.shootingHeight,
                bot.batteryMonitor?.voltage ?: 12.0  // ✅ NEW: Pass battery voltage
            ) + flywheelSpeedOffset
        }
    }

    private fun handleFlywheel() {
        flywheelToggle.toggle(gamepad.dpad_right)

        if (flywheelToggle.state) {
            // Shooting mode - set target velocity
            bot.flywheel?.targetVelocity = flywheelSpeed
            bot.flywheel?.state = Flywheel.FlywheelState.SHOOTING
        } else {
            // Handle different operating modes when toggle is OFF
            when (Constants.Flywheel.OPERATING_MODE) {
                FlywheelOperatingMode.ALWAYS_IDLE -> {
                    bot.flywheel?.state = Flywheel.FlywheelState.IDLE
                }
                FlywheelOperatingMode.SMART_IDLE -> {
                    // Smart mode handles itself - will go to OFF after timeout
                    bot.flywheel?.state = Flywheel.FlywheelState.IDLE
                }
                FlywheelOperatingMode.CONSERVATIVE_IDLE -> {
                    // Conservative mode - run at lower idle speed
                    bot.flywheel?.state = Flywheel.FlywheelState.IDLE
                }

                FlywheelOperatingMode.FULL_OFF -> {
                    bot.flywheel?.state = Flywheel.FlywheelState.OFF
                }
            }
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

        }
    }

    private fun handleMultiShot() {
        // Start multi-shot sequences
        if (multiShotState == MultiShotState.IDLE) {
            if (gamepad.circle) {
                startMultiShot(fastMode = false)
            } else if (gamepad.options) {
                startMultiShot(fastMode = true)
            }
        }

        // Process active multi-shot
        when (multiShotState) {
            MultiShotState.IDLE -> {
                multiShotStatus = ""
            }

            MultiShotState.WAITING_FOR_LAUNCHER -> {
                multiShotStatus = "Multi-shot ${if (multiShotUseFastMode) "[FAST]" else "[SAFE]"}: Shot $multiShotCount/$multiShotTarget - Waiting for launcher..."

                // Wait for launcher to reset
                if (bot.launcher?.isReset == true) {
                    // ✅ NEW: Log shot completion
                    logShotComplete()

                    // Pop the artifact we just shot
                    bot.spindexer?.popCurrentArtifact(false)

                    // Check if we're done
                    if (multiShotCount >= multiShotTarget) {
                        completeMultiShot()
                    } else {
                        // Move to next artifact
                        val moved = bot.spindexer?.moveToNextOuttake()
                        if (moved == true) {
                            multiShotState = MultiShotState.WAITING_FOR_SPINDEXER
                            multiShotTimer.reset()
                        } else {
                            // No more artifacts available
                            completeMultiShot()
                        }
                    }
                }
            }

            MultiShotState.WAITING_FOR_SPINDEXER -> {
                multiShotStatus = "Multi-shot ${if (multiShotUseFastMode) "[FAST]" else "[SAFE]"}: Shot $multiShotCount/$multiShotTarget - Positioning..."

                // Wait for spindexer to reach position
                val ready = if (multiShotUseFastMode) {
                    // Fast mode: just check position tolerance
                    bot.spindexer?.withinDetectionTolerance == true &&
                            multiShotTimer.milliseconds() > 100 // Small delay for stability
                } else {
                    // Safe mode: wait for full settle
                    bot.spindexer?.reachedTarget == true
                }

                if (ready) {
                    // Fire next shot
                    bot.launcher?.triggerLaunch()
                    multiShotCount++
                    currentShotNumber++
                    shootingArtifact = true

                    // ✅ NEW: Log shot start
                    logShotStart()

                    multiShotState = MultiShotState.WAITING_FOR_LAUNCHER
                }
            }

            MultiShotState.COMPLETE -> {
                // This state is just for one cycle to allow completion actions
                multiShotState = MultiShotState.IDLE
            }
        }
    }

    private fun startMultiShot(fastMode: Boolean) {
        // Make sure flywheel is running
        if (!flywheelToggle.state) {
            flywheelToggle.state = true
            bot.flywheel?.targetVelocity = flywheelSpeed
            bot.flywheel?.state = Flywheel.FlywheelState.SHOOTING
        }

        // Check how many artifacts we have
        val numArtifacts = bot.spindexer?.numStoredArtifacts ?: 0
        if (numArtifacts == 0) {
            multiShotStatus = "Multi-shot: No artifacts loaded!"
            return
        }

        // Determine target (shoot up to 3, but only what we have)
        multiShotTarget = minOf(3, numArtifacts)
        multiShotCount = 0
        multiShotUseFastMode = fastMode

        // Make sure we're at an outtake position
        if (bot.spindexer?.isOuttakePosition != true) {
            bot.spindexer?.moveToNextOuttake()
            multiShotState = MultiShotState.WAITING_FOR_SPINDEXER
            multiShotTimer.reset()
        } else {
            // Already positioned, fire first shot immediately
            bot.launcher?.triggerLaunch()
            multiShotCount++
            currentShotNumber++
            shootingArtifact = true

            // ✅ NEW: Log shot start
            logShotStart()

            multiShotState = MultiShotState.WAITING_FOR_LAUNCHER
        }

        multiShotStatus = "Multi-shot ${if (fastMode) "[FAST]" else "[SAFE]"}: Starting ($multiShotTarget artifacts)"
    }

    private fun completeMultiShot() {
        multiShotState = MultiShotState.COMPLETE
        multiShotStatus = "Multi-shot complete! ($multiShotCount shots)"

        // Rumble gamepad to indicate completion
        gamepad.rumble(200)
    }

    private fun processShooting() {
        if (shootingArtifact && bot.launcher?.isReset == true ) {
            shootingArtifact = false
            // Only pop if we're NOT in a multi-shot sequence
            // (multi-shot handles its own popping)
            if (multiShotState == MultiShotState.IDLE) {
                // ✅ NEW: Log single shot completion
                logShotComplete()

                bot.spindexer?.popCurrentArtifact(false)
            }
        }
        if (!flywheelToggle.state) return
        launchToggle.toggle(gamepad.square)
        if (launchToggle.justChanged &&
            bot.spindexer?.withinDetectionTolerance == true &&
            bot.spindexer?.isOuttakePosition == true
        ) {
            bot.launcher?.triggerLaunch()
            currentShotNumber++
            shootingArtifact = true

            // ✅ NEW: Log single shot start
            logShotStart()
        } else if (launchToggle.justChanged && launchPressedTimer.seconds() < 0.5) {
            bot.launcher?.triggerLaunch()
            currentShotNumber++
            shootingArtifact = true

            // ✅ NEW: Log single shot start
            logShotStart()
        }
        if (launchToggle.justChanged) launchPressedTimer.reset()
    }

    private fun shootArtifact(artifact: Artifact? = null) {
        val moved = if (artifact != null) {
            bot.spindexer?.moveToNextOuttake(artifact)
        } else {
            bot.spindexer?.moveToNextOuttake()
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
            bot.turret?.autoTrack(
                bot.pinpoint?.pose ?: Pose(),
                finalShootingTarget,
            )
        }
        if (abs(gamepad.right_stick_x) > 0.02) {
            useAutoTrackOffset = true
            offsetShootingTarget = offsetShootingTarget.rotate(-gamepad.right_stick_x.toDouble().pow(3) / 17.5)
        }
        if (gamepad.right_stick_button){
            useAutoTrackOffset = false
            offsetShootingTarget = tagShootingTarget
        }
        if (useAutoTrackOffset){
            finalShootingTarget = offsetShootingTarget
        } else {
            finalShootingTarget = tagShootingTarget
        }
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

    // ========================================
    // ✅ NEW: Test matrix logging helpers
    // ========================================

    private fun logShotStart() {
        totalShotsFired++

        testLogger?.let { logger ->
            val distance = bot.pinpoint?.pose?.distanceTo(targetGoal.shootingPose) ?: 0.0

            logger.logShot(
                shotNumber = currentShotNumber,
                distance = distance,
                targetX = targetGoal.shootingPose.x,
                targetY = targetGoal.shootingPose.y,
                hit = false  // Will update on completion if we add hit detection
            )

            // Log flywheel metrics
            logger.logMetric("flywheel_target_speed", flywheelSpeed, "tps", "PLCS")
            logger.logMetric("flywheel_actual_speed", bot.flywheel?.velocity ?: 0.0, "tps", "PLCS")
            logger.logMetric("shot_distance", distance, "cm", "PLCS")

            // Log shot quality
            val quality = bot.flywheel?.assessShotQuality(distance)
            logger.logEvent("SHOT_QUALITY", mapOf(
                "distance" to "%.1f".format(distance),
                "quality" to quality.toString()
            ))
        }
    }

    private fun logShotComplete() {
        testLogger?.let { logger ->
            logger.logEvent("SHOT_COMPLETE", mapOf(
                "shot_number" to currentShotNumber.toString()
            ))
        }
    }
}