package org.firstinspires.ftc.teamcode.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import org.firstinspires.ftc.teamcode.GoBildaStarterBot
import org.firstinspires.ftc.teamcode.helpers.Toggle
import org.firstinspires.ftc.teamcode.localization.Pose

class TeleopDriver1 (var gamepad: Gamepad, val bot: GoBildaStarterBot) {
    var driveSpeed = 0.5
    val fieldCentric: Boolean
        get() = fieldCentricToggle.state

    // Toggles
    private var incDriveSpeed: Toggle = Toggle(false)
    private var decDriveSpeed: Toggle = Toggle(false)
    private var fieldCentricToggle: Toggle = Toggle(false)

    fun update() {
        drive()
        updateDriveSpeed()
        updateFieldCentric()
        flywheelSpeed()
        updateLaunchServos()
    }

    fun drive() {
        val direction = Pose(gamepad.left_stick_x.toDouble(), -gamepad.left_stick_y.toDouble())
        bot.mecanumBase.setDrivePower(
            direction.x,
            direction.y,
            gamepad.right_stick_x.toDouble(),
            driveSpeed
        )
    }

    fun updateDriveSpeed() {
        incDriveSpeed.toggle(gamepad.right_bumper)
        decDriveSpeed.toggle(gamepad.left_bumper)
        if (incDriveSpeed.justChanged) {
            driveSpeed += 0.1
        }
        if (decDriveSpeed.justChanged) {
            driveSpeed -= 0.1
        }
        driveSpeed = driveSpeed.coerceIn(0.1, 1.0)
    }

    fun updateFieldCentric() {
        fieldCentricToggle.toggle(gamepad.left_trigger > 0.5 && gamepad.right_trigger > 0.5)
    }

    fun flywheelSpeed() {
        if (gamepad.circle) {
            bot.flywheel.setSpeed(0.67)
        } else {
            bot.flywheel.setSpeed(0.0)
        }
    }

    fun updateLaunchServos() {
        if (gamepad.dpad_up) {
            bot.launchServos.triggerLaunch()
        }
        else if (gamepad.dpad_down) {
            bot.launchServos.triggerRetract()
        }
        bot.launchServos.update()
    }
}
