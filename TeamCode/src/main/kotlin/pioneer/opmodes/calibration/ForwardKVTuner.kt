package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import pioneer.Bot
import pioneer.helpers.Pose

@Autonomous(name = "Forward KV Tuner", group = "Calibration")
class ForwardKVTuner : OpMode() {
    private lateinit var bot: Bot

    override fun init() {
        bot = Bot(pioneer.BotType.BASIC_MECANUM_BOT, hardwareMap)
    }

    override fun loop() {
        bot.dtTracker.update()
        bot.localizer.update(bot.dtTracker.dt)
        bot.mecanumBase.setDriveVA(
            Pose(vy = 50.0, ay = 0.0), // 50 cm/s forward
        )
        telemetry.addData("Velocity (cm/s)", bot.localizer.pose.vy)
        telemetry.addData("Position (cm)", bot.localizer.pose.y)
        telemetry.update()
    }
}
