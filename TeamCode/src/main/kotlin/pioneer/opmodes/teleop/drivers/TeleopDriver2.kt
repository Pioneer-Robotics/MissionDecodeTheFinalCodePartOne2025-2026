package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import pioneer.Bot
import pioneer.decode.Artifact
import pioneer.hardware.Turret
import pioneer.helpers.Chrono
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import pioneer.helpers.next
import kotlin.math.PI
import kotlin.math.abs

class TeleopDriver2(
    private val gamepad: Gamepad,
    private val bot: Bot,
) {

    enum class FlywheelSpeed(val speed: Double) {
        SHORT_RANGE(0.75),
        LONG_RANGE(1.0),
    }

    private val chrono = Chrono(autoUpdate = false)
    private val isAutoTracking = Toggle(false)
    private val flywheelToggle = Toggle(false)
    private val changeFlywheelSpeedToggle = Toggle(false)

    enum class ShootState { READY, MOVING_TO_POSITION, LAUNCHING }
    var flywheelSpeedEnum = FlywheelSpeed.SHORT_RANGE
    var flywheelSpeed = 0.7
    var shootState = ShootState.READY
    private var shootingAll = false
    private var remainingShots = 0
    var turretAngle = 0.0

    fun update() {
        updateFlywheelSpeed()
        handleFlywheel()
        handleTurret()
        handleShootInput()
        processShooting()
        chrono.update() // Manual update to allow dt to match across the loop.

        if (gamepad.square) bot.launcher?.triggerLaunch()
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
            flywheelSpeedEnum = flywheelSpeedEnum.next()
            flywheelSpeed = flywheelSpeedEnum.speed
        }
    }

    private fun handleFlywheel() {
        flywheelToggle.toggle(gamepad.dpad_left)
        if (flywheelToggle.state) {
            bot.flywheel?.velocity = -flywheelSpeed * 1200
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
                gamepad.touchpad -> startShootingAll()
            }
        }
    }

    private fun processShooting() {
        when (shootState) {
            ShootState.MOVING_TO_POSITION -> {
                if (bot.spindexer?.reachedTarget == true) {
                    bot.launcher?.triggerLaunch()
                    bot.spindexer?.popCurrentArtifact()
                    shootState = ShootState.LAUNCHING
                }
            }
            ShootState.LAUNCHING -> {
                if (bot.launcher?.isReset == true) {
                    if (shootingAll && remainingShots > 0) {
                        shootNextForAll()
                    } else {
                        shootState = ShootState.READY
                        shootingAll = false
                    }
                }
            }
            else -> {}
        }
    }

    private fun shootArtifact(artifact: Artifact? = null) {
        val moved = if (artifact != null) {
            bot.spindexer?.moveToNextOuttake(artifact)
        } else {
            bot.spindexer?.moveToNextOuttake()
        }
        if (moved == true) shootState = ShootState.MOVING_TO_POSITION
    }

    private fun startShootingAll() {
        shootingAll = true
        remainingShots = bot.spindexer?.numStoredArtifacts ?: 0
        if (remainingShots > 0) {
            shootNextForAll()
        } else {
            shootingAll = false
        }
    }

    private fun shootNextForAll() {
        if (remainingShots > 0) {
            remainingShots--
            shootArtifact()
            shootState = ShootState.MOVING_TO_POSITION
        } else {
            shootingAll = false
            shootState = ShootState.READY
        }
    }

    private fun handleManualTrack() {
        if (abs(gamepad.right_stick_x) > 0.02) {
            turretAngle -= gamepad.right_stick_x.toDouble() * chrono.dt
            turretAngle.coerceIn(
                -PI,
                PI
            ) // FIX: This will break if the turret has a different range. Try to hand off this to the Turret class
        }
        bot.turret?.gotoAngle(turretAngle)
    }

    private fun handleAutoTrack() {
        if (bot.turret?.mode == Turret.Mode.AUTO_TRACK) {
            bot.turret?.autoTrack(bot.pinpoint?.pose ?: Pose(), Pose(10.0,10.0))
        }
    }
}
