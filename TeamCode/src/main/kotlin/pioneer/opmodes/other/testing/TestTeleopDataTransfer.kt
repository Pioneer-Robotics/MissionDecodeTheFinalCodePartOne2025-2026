package pioneer.opmodes.other.testing

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Bot
import pioneer.BotType
import pioneer.general.AllianceColor
import pioneer.general.Period
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode

@TeleOp(name = "Test Teleop Data Transfer", group = "Testing")
class TestTeleopDataTransfer : BaseOpMode(
    period = Period.TELEOP, 
    allianceColor = AllianceColor.RED
) {

    override fun onInit() {
        // Bot is already loaded by BaseOpMode if it exists from AUTO
        // Otherwise, we need to create a new one
        // We can check by looking at bot.type or just always show status
        
        telemetry.addLine("=== BOT INITIALIZATION ===")
        telemetry.addLine("Bot loaded - Check if from AUTO or new")
        telemetry.addData("Bot Type", bot.type)
        bot.pinpoint?.pose?.let { pose ->
            telemetry.addData("Starting Pose", "x=%.1f, y=%.1f, θ=%.1f°".format(
                pose.x, pose.y, pose.theta
            ))
        }
        telemetry.addLine()
        telemetry.addLine("If this is a default pose, bot was created new")
        telemetry.addLine("If this is your AUTO end pose, bot was loaded!")
        telemetry.addLine()
        telemetry.addLine("Use left stick to drive")
        telemetry.update()
    }

    override fun onLoop() {
        // Simple drive controls for testing
        drive()
        
        // Display bot status
        telemetry.addLine("=== BOT STATUS ===")
        telemetry.addData("Bot Type", bot.type)
        
        bot.pinpoint?.let { pinpoint ->
            telemetry.addLine()
            telemetry.addLine("--- CURRENT STATE ---")
            telemetry.addData("Current Pose", "x=%.1f, y=%.1f, θ=%.1f°",
                pinpoint.pose.x, pinpoint.pose.y, pinpoint.pose.theta)
        }
        
        telemetry.addLine()
        telemetry.addLine("Drive: Left stick | Turn: Right stick X")
        telemetry.update()
    }

    private fun drive() {
        val direction = Pose(
            gamepad1.left_stick_x.toDouble(), 
            -gamepad1.left_stick_y.toDouble()
        )
        bot.mecanumBase?.setDrivePower(
            Pose(
                vx = direction.x,
                vy = direction.y,
                omega = gamepad1.right_stick_x.toDouble(),
            ),
            0.5, // Default power
            1000.0, // Max velocity
        )
    }
}
