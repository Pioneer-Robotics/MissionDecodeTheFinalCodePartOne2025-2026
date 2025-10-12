package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import pioneer.Bot
import pioneer.helpers.Toggle
import pioneer.localization.Pose

class TeleopDriver1 (var gamepad: Gamepad) {
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
    }

    fun drive() {
        val direction = Pose(gamepad.left_stick_x.toDouble(), -gamepad.left_stick_y.toDouble())
        if (fieldCentricToggle.state) direction.rotate(Bot.localizer.pose.heading) // Rotate to local coordinates

        Bot.mecanumBase.setDrivePower(
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
}
