package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import pioneer.Bot
import pioneer.helpers.Pose

@Autonomous(name = "Rotational KV Tuner", group = "Calibration")
class RotationalKVTuner : OpMode() {
    private lateinit var bot: Bot

    override fun init() {
        bot = Bot(pioneer.BotType.BASIC_MECANUM_BOT, hardwareMap)
    }

    override fun loop() {
        bot.dtTracker.update()
        bot.localizer.update(bot.dtTracker.dt)
        bot.mecanumBase.setDriveVA(
            Pose(omega = 1.0, alpha = 0.0),  // 1 rad/s clockwise
        )
        telemetry.addData("Velocity (cm/s)", bot.localizer.pose.omega)
        telemetry.addData("Position (cm)", bot.localizer.pose.theta)
        telemetry.update()
    }
}
