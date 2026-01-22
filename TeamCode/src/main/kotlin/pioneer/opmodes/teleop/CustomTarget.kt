package pioneer.opmodes.teleop

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Bot
import pioneer.BotType
import pioneer.helpers.Toggle
import pioneer.opmodes.BaseOpMode

@TeleOp(name="Custom Target")
class CustomTarget : BaseOpMode() {
    var targetDistance = 0.0
    var targetHeight = 0.0
    var targetAngle = 0.0
    var selection = false

    val moveUpToggle = Toggle(false)
    val moveDownToggle = Toggle(false)
    val increaseToggle = Toggle(false)
    val decreaseToggle = Toggle(false)

    override fun onInit() {
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)
    }

    override fun onLoop() {
        moveUpToggle.toggle(gamepad1.dpad_up)
        moveDownToggle.toggle(gamepad1.dpad_down)
        if (moveUpToggle.justChanged) selection = true
        if (moveDownToggle.justChanged) selection = false

        increaseToggle.toggle(gamepad1.dpad_right)
        decreaseToggle.toggle(gamepad1.dpad_left)
        if (increaseToggle.justChanged) {
            if (selection) {
                targetDistance += 1.0
            } else {
                targetHeight += 1.0
            }
        }
        if (decreaseToggle.justChanged) {
            if (selection) {
                targetDistance -= 1.0
            } else {
                targetHeight -= 1.0
            }
        }
        telemetry.addLine((if (selection) ">" else " ") + " Target Distance: $targetDistance")
        telemetry.addLine((if (!selection) ">" else " ") + " Target Height: $targetHeight")
    }
}