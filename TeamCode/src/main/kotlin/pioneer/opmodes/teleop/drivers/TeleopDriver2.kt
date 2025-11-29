package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import pioneer.Bot
import pioneer.decode.Artifact
import pioneer.helpers.Chrono
import pioneer.helpers.Pose
import kotlin.math.PI
import kotlin.math.abs

class TeleopDriver2(
    private val gamepad: Gamepad,
    private val bot: Bot,
) {
    private val chrono = Chrono()

    private enum class ShootState { READY, MOVING_TO_POSITION, LAUNCHING }

    var flywheelSpeed = 0.7 * 1200
    private var autoTrack = false
    private var shootState = ShootState.READY
    private var shootingAll = false
    private var remainingShots = 0

    fun update() {
        updateFlywheelSpeed()
        handleFlywheel()
        handleShootInput()
        processShooting()
        handleAutoTrack()
    }

    private fun updateFlywheelSpeed() {
        if (flywheelSpeed < 1.0 && gamepad.dpad_right) {
            flywheelSpeed += 0.25 * chrono.dt / 1000
        }
        if (flywheelSpeed > 0.0 && gamepad.dpad_left) {
            flywheelSpeed -= 0.25 * chrono.dt / 1000
        }
        flywheelSpeed = flywheelSpeed.coerceIn(0.0, 1.0)
    }

    private fun handleFlywheel() {
        if (gamepad.dpad_up) {
            bot.flywheel?.velocity = -flywheelSpeed
            autoTrack = true
        } else {
            autoTrack = false
            bot.flywheel?.velocity = 0.0
        }
    }

    private fun handleTurret() {
        if (abs(gamepad.right_stick_x) > 0.05) {
            autoTrack = false
            bot.turret?.gotoAngle(gamepad.right_stick_x.toDouble() * PI)
        } else {
            bot.turret?.gotoAngle(0.0)
        }
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

    private fun handleAutoTrack() {
        if (autoTrack) {
            bot.turret?.autoTrack(bot.pinpoint?.pose ?: Pose(), Pose())
        } else {
            handleTurret()
        }
    }
}
