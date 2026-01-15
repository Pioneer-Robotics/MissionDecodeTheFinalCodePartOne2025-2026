package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import pioneer.Bot
import pioneer.decode.Artifact
import pioneer.decode.GoalTag
import pioneer.general.AllianceColor
import pioneer.hardware.Turret
import pioneer.helpers.Chrono
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import kotlin.math.*

class TeleopDriver2(
    private val gamepad: Gamepad,
    private val bot: Bot,
) {
    private val chrono = Chrono(autoUpdate = false)
    private val isAutoTracking = Toggle(false)
    private val isEstimatingSpeed = Toggle(true)
    private val flywheelToggle = Toggle(false)
    var targetGoal = GoalTag.RED
    var turretAngle = 0.0
    var flywheelSpeed = 0.0
    var manualFlywheelSpeed = 0.0

    fun update() {
        checkTargetGoal()
        updateFlywheelSpeed()
        handleFlywheel()
        handleTurret()
        handleShootInput()
        processShooting()
        updateIndicatorLED()
    }

    private fun checkTargetGoal() {
        if (bot.allianceColor == AllianceColor.BLUE) {
            targetGoal = GoalTag.BLUE
        } else { return }
    }

    private fun updateFlywheelSpeed() {
        isEstimatingSpeed.toggle(gamepad.dpad_right)
        if (!isEstimatingSpeed.state){
            if (gamepad.dpad_up){
                manualFlywheelSpeed += 50.0
            }
            if (gamepad.dpad_down){
                manualFlywheelSpeed -= 50.0
            }
            flywheelSpeed = manualFlywheelSpeed
        } else {
            flywheelSpeed = bot.flywheel!!.estimateVelocity(bot.pinpoint?.pose ?: Pose(), targetGoal.shootingPose, targetGoal.shootingHeight)
        }
    }

    private fun handleFlywheel() {
        flywheelToggle.toggle(gamepad.dpad_left)
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
        if (!flywheelToggle.state) return
        if (gamepad.square &&
            bot.spindexer?.reachedTarget == true &&
            bot.spindexer?.isOuttakePosition == true
        ) {
            bot.launcher?.triggerLaunch()
            bot.spindexer?.popCurrentArtifact()
        }
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
            turretAngle -= gamepad.right_stick_x.toDouble().pow(3) * chrono.dt/1000.0 * 5.0
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
                targetGoal.shootingPose,
            )
        }
    }

    private fun updateIndicatorLED() {
        bot.flywheel?.velocity?.let {
            if (abs(flywheelSpeed - it) < 20.0) {
                gamepad.setLedColor(0.0, 1.0, 0.0, -1)
            } else if (it < flywheelSpeed - 20.0){
                gamepad.setLedColor(255.0,165.0,0.0, -1)
            }
            else {
                gamepad.setLedColor(1.0, 0.0, 0.0, -1)
            }
        }
    }
}
