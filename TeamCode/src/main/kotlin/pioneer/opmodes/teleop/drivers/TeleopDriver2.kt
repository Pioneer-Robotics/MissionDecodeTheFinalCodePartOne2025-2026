package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import pioneer.Bot
import pioneer.decode.Artifact
import pioneer.decode.GoalTag
import pioneer.decode.Points
import pioneer.general.AllianceColor
import pioneer.hardware.Turret
import pioneer.helpers.Chrono
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import pioneer.helpers.next
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow

class TeleopDriver2(
    private val gamepad: Gamepad,
    private val bot: Bot,
) {
    enum class FlywheelSpeed(
        val velocity: Double,
    ) {
        SHORT_RANGE(800.0),
        LONG_RANGE(1000.0),
    }

    private val chrono = Chrono(autoUpdate = false)
    private val isAutoTracking = Toggle(false)
    private val flywheelToggle = Toggle(false)
    private val changeFlywheelSpeedToggle = Toggle(false)

    var P = Points(bot.allianceColor)

    enum class ShootState { READY, MOVING_TO_POSITION, LAUNCHING }

    var flywheelVelocityEnum = FlywheelSpeed.SHORT_RANGE
    var shootState = ShootState.READY
    var targetGoal = Pose()
    private var shootingAll = false
    private var remainingShots = 0
    var turretAngle = 0.0

    fun update() {
        updateFlywheelSpeed()
        handleFlywheel()
        handleTurret()
        handleShootInput()
        processShooting()
        updateIndicatorLED()
        chrono.update() // Manual update to allow dt to match across the loop.
    }

    private fun updateFlywheelSpeed() {
//        if (flywheelSpeed < 1.0 && gamepad.dpad_up) {
//            flywheelSpeed += chrono.dt * 0.5
//        }
//        if (flywheelSpeed > 0.0 && gamepad.dpad_down) {
//            flywheelSpeed -= chrono.dt * 0.5
//        }
//        flywheelSpeed = flywheelSpeed.coerceIn(0.0, 1.0)
        changeFlywheelSpeedToggle.toggle(gamepad.dpad_up)
        if (changeFlywheelSpeedToggle.justChanged) {
            flywheelVelocityEnum = flywheelVelocityEnum.next()
        }
    }

    private fun handleFlywheel() {
        flywheelToggle.toggle(gamepad.dpad_left)
        if (flywheelToggle.state) {
            bot.flywheel?.velocity = flywheelVelocityEnum.velocity
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
        if (shootState == ShootState.READY && !shootingAll) {
            when {
                gamepad.right_bumper -> shootArtifact(Artifact.PURPLE)
                gamepad.left_bumper -> shootArtifact(Artifact.GREEN)
                gamepad.triangle -> shootArtifact()
//                gamepad.touchpad -> startShootingAll()
//                gamepad.touchpad -> startShootingAll()
            }
        }
    }

    private fun processShooting() {
//        when (shootState) {
//            ShootState.MOVING_TO_POSITION -> {
//                if (bot.spindexer?.reachedTarget == true) {
//                    bot.launcher?.triggerLaunch()
//                    bot.spindexer?.popCurrentArtifact()
//                    shootState = ShootState.LAUNCHING
//                }
//            }
//            ShootState.LAUNCHING -> {
//                if (bot.launcher?.isReset == true) {
//                    if (shootingAll && remainingShots > 0) {
//                        shootNextForAll()
//                    } else {
//                        shootState = ShootState.READY
//                        shootingAll = false
//                    }
//                }
//            }
//            else -> {}
//        }
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
//        if (moved == true) shootState = ShootState.MOVING_TO_POSITION
    }

//    private fun startShootingAll() {
//        shootingAll = true
//        remainingShots = bot.spindexer?.numStoredArtifacts ?: 0
//        if (remainingShots > 0) {
//            shootNextForAll()
//        } else {
//            shootingAll = false
//        }
//    }

//    private fun shootNextForAll() {
//        if (remainingShots > 0) {
//            remainingShots--
//            shootArtifact()
//            shootState = ShootState.MOVING_TO_POSITION
//        } else {
//            shootingAll = false
//            shootState = ShootState.READY
//        }
//    }

    private fun handleManualTrack() {
        if (abs(gamepad.right_stick_x) > 0.02) {
            turretAngle -= gamepad.right_stick_x.toDouble().pow(3) * chrono.dt * 2.5
            turretAngle.coerceIn(
                -PI,
                PI,
            ) // FIX: This will break if the turret has a different range. Try to hand off this to the Turret class
        }

        if (gamepad.right_stick_button) {
            turretAngle = 0.0
        }
        bot.turret?.gotoAngle(turretAngle)
    }

    private fun handleAutoTrack() {
        if (bot.turret?.mode == Turret.Mode.AUTO_TRACK) {
            if (bot.allianceColor == AllianceColor.RED) {
                targetGoal = GoalTag.RED.pose
            } else {
                targetGoal = GoalTag.BLUE.pose
            }
            bot.turret?.autoTrack(
                bot.pinpoint?.pose ?: Pose(),
                if (bot.allianceColor ==
                    AllianceColor.BLUE
                ) {
                    GoalTag.BLUE.shootingPose
                } else {
                    GoalTag.RED.shootingPose
                },
            )
        }
    }

    private fun updateIndicatorLED() {
        bot.flywheel?.velocity?.let {
            if (it > flywheelVelocityEnum.velocity) {
                gamepad.setLedColor(0.0, 1.0, 0.0, -1)
            } else {
                gamepad.setLedColor(1.0, 0.0, 0.0, -1)
            }
        }
    }
}
