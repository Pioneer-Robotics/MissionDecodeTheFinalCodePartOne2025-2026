package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import pioneer.Bot
import pioneer.helpers.Pose

@Autonomous(name = "Horizontal KV Tuner", group = "Calibration")
class HorizontalKVTuner : OpMode() {
    private lateinit var bot: Bot

    override fun init() {
        bot = Bot(pioneer.BotType.BASIC_MECANUM_BOT, hardwareMap)
    }

    override fun loop() {
        bot.dtTracker.update()
        bot.localizer.update(bot.dtTracker.dt)
        bot.mecanumBase.setDriveVA(
            Pose(vx = 50.0, ax = 0.0), // 50 cm/s right
        )
        telemetry.addData("Velocity (cm/s)", bot.localizer.pose.vx)
        telemetry.addData("Position (cm)", bot.localizer.pose.x)
        telemetry.update()
    }
}
