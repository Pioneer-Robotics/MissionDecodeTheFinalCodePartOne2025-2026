package org.firstinspires.ftc.teamcode.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import org.firstinspires.ftc.teamcode.Bot
import org.firstinspires.ftc.teamcode.localization.Pose

@Autonomous(name = "Rotational KV Tuner", group = "Calibration")
class RotationalKVTuner : OpMode() {

    private lateinit var bot: Bot

    override fun init() {
        bot = Bot(Bot.BotFlavor.GOBILDA_STARTER_BOT, hardwareMap)
    }

    override fun loop() {
        bot.update()
        bot.mecanumBase.setDriveVA(
            Pose(0.0, 0.0, 1.0),    // 1 rad/s rotation
            Pose(0.0, 0.0, 0.0)     // No acceleration, we are only tuning velocity
        )
        telemetry.addData("Velocity (rad/s)", bot.localizer.velocity.heading)
        telemetry.addData("Rotation (rad)", bot.localizer.pose.heading)
        telemetry.update()
    }
}
