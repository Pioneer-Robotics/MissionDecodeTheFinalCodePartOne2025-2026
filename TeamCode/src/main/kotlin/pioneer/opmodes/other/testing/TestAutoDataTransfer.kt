package pioneer.opmodes.other.testing

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.Bot
import pioneer.BotType
import pioneer.general.AllianceColor
import pioneer.general.Period
import pioneer.opmodes.BaseOpMode

@Autonomous(name = "Test Auto Data Transfer", group = "Testing")
class TestAutoDataTransfer : BaseOpMode(
    period = Period.AUTO, 
    allianceColor = AllianceColor.RED
) {
    
    override fun onInit() {
        bot = Bot.fromType(BotType.GOBILDA_STARTER_BOT, hardwareMap)
        telemetry.addLine("Test Auto - Will save bot for teleop")
        telemetry.addLine("Press START to run")
        telemetry.update()
    }

    override fun onLoop() {
        val currentPose = bot.pinpoint?.pose
        
        telemetry.addLine("✓ Bot will auto-save on stop!")
        telemetry.addLine()
        telemetry.addData("Alliance", "RED")
        telemetry.addData("Bot Type", bot.type)
        if (currentPose != null) {
            telemetry.addData("Current Pose", "x=%.1f, y=%.1f, θ=%.1f°", 
                currentPose.x, currentPose.y, currentPose.theta)
        }
        telemetry.addLine()
        telemetry.addLine("Stop OpMode to save bot, then run Test Teleop")
        telemetry.update()
    }

    override fun onStop() {
        telemetry.addLine("Auto stopped - Bot saved by BaseOpMode")
        telemetry.update()
    }
}
