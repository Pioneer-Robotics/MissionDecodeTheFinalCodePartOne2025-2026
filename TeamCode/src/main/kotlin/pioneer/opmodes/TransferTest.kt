package pioneer.opmodes

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Bot
import pioneer.BotType
import pioneer.Constants

@TeleOp(name="Transfer Test", group="Testng")
class TransferTest : BaseOpMode() {
    override fun onInit() {
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)
    }

    override fun onLoop() {
        if (gamepad1.right_trigger > 0.1) {
            bot.turret?.gotoAngle(gamepad1.right_trigger.toDouble())
        }
        if (gamepad1.left_trigger > 0.1) {
            bot.spindexer?.moveManual(gamepad1.left_trigger.toDouble())
        }
        Constants.TransferData.spindexerPositionTicks = bot.spindexer?.currentMotorPosition ?: 0
        Constants.TransferData.turretPositionTicks = bot.turret?.currentTicks ?: 0
    }
}