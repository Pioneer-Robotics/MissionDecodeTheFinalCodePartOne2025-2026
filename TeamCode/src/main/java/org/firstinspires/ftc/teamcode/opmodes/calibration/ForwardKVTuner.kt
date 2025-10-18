package org.firstinspires.ftc.teamcode.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import org.firstinspires.ftc.teamcode.Bot
import org.firstinspires.ftc.teamcode.localization.Pose

@Autonomous(name = "Forward KV Tuner", group = "Calibration")
class ForwardKVTuner : OpMode() {
    private lateinit var bot: Bot

    override fun init() {
        bot = Bot(Bot.BotFlavor.GOBILDA_STARTER_BOT, hardwareMap)
    }

    override fun loop() {
        bot.update()
        bot.mecanumBase.setDriveVA(
            Pose(0.0, 50.0, 0.0),   // 50 cm/s forward
            Pose(0.0, 0.0, 0.0)     // No acceleration, we are only tuning velocity
        )
        telemetry.addData("Velocity (cm/s)", bot.localizer.velocity.y)
        telemetry.addData("Position (cm)", bot.localizer.pose.y)
        telemetry.update()
    }
}
