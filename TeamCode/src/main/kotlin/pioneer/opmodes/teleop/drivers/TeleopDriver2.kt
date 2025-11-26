package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Bot
import pioneer.helpers.Chrono

class TeleopDriver2(
    val gamepad: Gamepad,
    val bot: Bot,
) {
    private val chrono = Chrono()
    private val shootTimer = ElapsedTime()

    private enum class ShootState {
        READY,
        BUSY
    }

    // Other variables
    private var autoTrack = false
    var flywheelSpeed = 0.7
    private var shootState = ShootState.READY

    fun update() {
        updateFlywheelSpeed()
        flywheel()
        updateManualTurret()
        handleShoot()

        handleAutoTrack()
    }

    private fun updateFlywheelSpeed() {
        if (flywheelSpeed < 1.0 && gamepad.dpad_right) {
            flywheelSpeed += 0.25 * chrono.dt / 1000
        }
        if (flywheelSpeed > 0.0 && gamepad.dpad_left) {
            flywheelSpeed -= 0.25 * chrono.dt / 1000
        }
        flywheelSpeed.coerceIn(0.0, 1.0)
    }

    private fun flywheel() {
        if (gamepad.dpad_up) {
            bot.flywheel?.power = -flywheelSpeed
            autoTrack = true
        } else {
            autoTrack = false
            bot.flywheel?.power = 0.0
        }
    }

    private fun updateManualTurret() {
        if (gamepad.right_stick_x < 0.01) {
            autoTrack = false
            // TODO: Rotate turret
        }
    }

    private fun handleShoot() {
        if (shootState == ShootState.READY) {
            if (gamepad.right_bumper) {
                shootPurple()
            } else if (gamepad.left_bumper) {
                shootGreen()
            } else if (gamepad.triangle) {
                shootNext()
            } else if (gamepad.touchpad) {
                shootAll()
            }
        }
        if (shootTimer.milliseconds() > 500) {
            shootState = ShootState.READY
        }
    }

    private fun shootPurple() {
        shootState = ShootState.BUSY
        shootTimer.reset()
        // TODO: Shoot purple
    }

    private fun shootGreen() {
        shootState = ShootState.BUSY
        shootTimer.reset()
        // TODO: Shoot green
    }

    private fun shootNext() {
        shootState = ShootState.BUSY
        shootTimer.reset()
        // TODO: Shoot next in spindexer
    }

    private fun shootAll() {
        shootState = ShootState.BUSY
        shootTimer.reset()
        // TODO: Shoot all (might need more logic)
    }

    private fun handleAutoTrack() {
        if (autoTrack) {
            // TODO: Track target
//            bot.turret.autoTrack(bot.pinpoint.pose, )
            //Need New CV to be merged in for AprilTag position
        }
    }


}
