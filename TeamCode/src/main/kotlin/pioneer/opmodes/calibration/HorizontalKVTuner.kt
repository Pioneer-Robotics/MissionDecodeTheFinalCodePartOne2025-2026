package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.Bot
import pioneer.BotType
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode

@Autonomous(name = "Horizontal KV Tuner", group = "Calibration")
class HorizontalKVTuner : BaseOpMode() {
    override fun onInit() {
        bot = Bot.fromType(BotType.MECANUM_BOT, hardwareMap)
    }

    override fun onLoop() {
        bot.mecanumBase!!.setDriveVA(
            Pose(vx = 50.0, ax = 0.0), // 50 cm/s right
        )
        telemetry.addData("Velocity (cm/s)", bot.pinpoint!!.pose.vx)
        telemetry.addData("Position (cm)", bot.pinpoint!!.pose.x)
    }
}
